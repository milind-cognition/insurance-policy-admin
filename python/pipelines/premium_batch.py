"""
Parallelized Premium Batch Pipeline (replaces PREMBAT.cbl / PREMIUM-BATCH.jcl)

Migrated from:  cobol/programs/PREMBAT.cbl  (CA-7 Job# PAS0050)
Schedule:       Daily at 01:00 EST
Author:         Modernization — original by M. Kowalski (1998)

Replaces the sequential COBOL cursor loop with a pipeline that partitions
active policies by POLICY_TYPE and calculates premiums in parallel,
eliminating the ~4-hour sequential bottleneck.

Key improvements over legacy PREMBAT:
  - Parallel processing partitioned by policy_type (AUT, HOM, COM, LIF, HLT)
  - Decimal arithmetic (no floating-point) for financial accuracy
  - Configurable rating factors loaded from DB once, shared across workers
  - Structured logging and per-partition error tracking
  - Batch INSERT for throughput (configurable chunk size)
"""

from __future__ import annotations

import logging
import os
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

import sqlalchemy as sa
from sqlalchemy import text
from sqlalchemy.engine import Engine

logger = logging.getLogger(__name__)

DB_URL = os.getenv(
    "PAS_DB_URL",
    "db2+ibm_db://user:pass@localhost:50000/DBPD",
)
MAX_WORKERS = int(os.getenv("PAS_PREMIUM_WORKERS", "5"))
BATCH_SIZE = int(os.getenv("PAS_PREMIUM_BATCH_SIZE", "500"))

POLICY_TYPES = ["AUT", "HOM", "COM", "LIF", "HLT"]

BASE_RATES: dict[str, Decimal] = {
    "AUT": Decimal("850.00"),
    "HOM": Decimal("1200.00"),
    "COM": Decimal("5000.00"),
    "LIF": Decimal("400.00"),
    "HLT": Decimal("3500.00"),
}
DEFAULT_BASE_RATE = Decimal("1000.00")

TAX_RATE = Decimal("0.0350")
REGULATORY_SURCHARGE = Decimal("25.00")

TWO_PLACES = Decimal("0.01")
FOUR_PLACES = Decimal("0.0001")


@dataclass
class RatingFactors:
    """Rating factors loaded from DB2 reference tables."""

    territory: dict[str, Decimal] = field(default_factory=dict)
    class_code: dict[str, Decimal] = field(default_factory=dict)


@dataclass
class PartitionResult:
    """Aggregated result for one policy_type partition."""

    policy_type: str
    policies_read: int = 0
    policies_updated: int = 0
    policies_error: int = 0
    errors: list[str] = field(default_factory=list)


def _build_engine(db_url: str | None = None) -> Engine:
    url = db_url or DB_URL
    return sa.create_engine(url, pool_pre_ping=True)


def load_rating_factors(engine: Engine) -> RatingFactors:
    """Load territory and class-code factors from DB2 reference tables."""
    factors = RatingFactors()

    with engine.connect() as conn:
        rows = conn.execute(
            text(
                "SELECT TERRITORY_CODE, RATING_FACTOR "
                "FROM TERRITORY_FACTORS "
                "WHERE EFFECTIVE_DATE <= CURRENT_DATE "
                "ORDER BY TERRITORY_CODE"
            )
        ).fetchall()
        for row in rows:
            factors.territory[row[0].strip()] = Decimal(str(row[1]))

        rows = conn.execute(
            text(
                "SELECT CLASS_CODE, RATING_FACTOR "
                "FROM CLASS_CODE_FACTORS "
                "WHERE EFFECTIVE_DATE <= CURRENT_DATE "
                "ORDER BY CLASS_CODE"
            )
        ).fetchall()
        for row in rows:
            factors.class_code[row[0].strip()] = Decimal(str(row[1]))

    logger.info(
        "Loaded %d territory factors, %d class factors",
        len(factors.territory),
        len(factors.class_code),
    )
    return factors


def calculate_premium(
    policy_type: str,
    total_premium: Decimal,
    deductible: Decimal,
    coverage_limit: Decimal,
    territory_code: str | None,
    class_code: str | None,
    rating_factors: RatingFactors,
) -> dict[str, Decimal]:
    """Replicate PREMBAT 3200-CALCULATE-PREMIUM logic with Decimal precision."""
    base = BASE_RATES.get(policy_type, DEFAULT_BASE_RATE)

    territory_factor = Decimal("1.0000")
    if territory_code and territory_code.strip() in rating_factors.territory:
        territory_factor = rating_factors.territory[territory_code.strip()]

    class_factor = Decimal("1.0000")
    if class_code and class_code.strip() in rating_factors.class_code:
        class_factor = rating_factors.class_code[class_code.strip()]

    experience_mod = Decimal("1.0000")

    terr_premium = (base * territory_factor).quantize(TWO_PLACES, ROUND_HALF_UP)
    class_premium = (terr_premium * class_factor).quantize(TWO_PLACES, ROUND_HALF_UP)
    mod_premium = (class_premium * experience_mod).quantize(TWO_PLACES, ROUND_HALF_UP)
    tax_amount = (mod_premium * TAX_RATE).quantize(TWO_PLACES, ROUND_HALF_UP)
    surcharge = REGULATORY_SURCHARGE
    final_premium = mod_premium + tax_amount + surcharge

    return {
        "base_rate": base,
        "territory_factor": territory_factor,
        "class_factor": class_factor,
        "experience_mod": experience_mod,
        "schedule_mod": Decimal("1.0000"),
        "discount_pct": Decimal("0.00"),
        "surcharge_amt": surcharge,
        "tax_amt": tax_amount,
        "total_premium": final_premium,
    }


def _process_partition(
    policy_type: str,
    db_url: str,
    rating_factors_dict: dict[str, Any],
    batch_size: int,
    calc_date: str,
) -> PartitionResult:
    """Process all active policies for a single policy_type partition.

    Runs in a child process — creates its own DB engine.
    """
    logging.basicConfig(level=logging.INFO)
    log = logging.getLogger(f"premium_batch.{policy_type}")
    result = PartitionResult(policy_type=policy_type)

    factors = RatingFactors(
        territory={k: Decimal(v) for k, v in rating_factors_dict["territory"].items()},
        class_code={k: Decimal(v) for k, v in rating_factors_dict["class_code"].items()},
    )

    engine = _build_engine(db_url)

    select_sql = text(
        "SELECT p.POLICY_NUMBER, p.POLICY_TYPE, "
        "       p.TOTAL_PREMIUM, p.DEDUCTIBLE, p.COVERAGE_LIMIT, "
        "       p.EFFECTIVE_DATE, p.EXPIRY_DATE, "
        "       c.RATING_TERRITORY, c.CLASS_CODE "
        "FROM POLICIES p "
        "LEFT JOIN COVERAGES c "
        "  ON p.POLICY_NUMBER = c.POLICY_NUMBER AND c.SEQUENCE_NUM = 1 "
        "WHERE p.POLICY_STATUS = 'AC' "
        "  AND p.POLICY_TYPE = :policy_type "
        "ORDER BY p.POLICY_NUMBER"
    )

    insert_sql = text(
        "INSERT INTO PREMIUMS "
        "(POLICY_NUMBER, COVERAGE_SEQ, TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE, "
        " BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR, EXPERIENCE_MOD, "
        " SCHEDULE_MOD, DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT, "
        " TOTAL_PREMIUM, INSTALLMENT_CODE, CALC_DATE, CALC_BY) "
        "VALUES "
        "(:policy_number, 1, :eff_date, :exp_date, "
        " :base_rate, :territory_factor, :class_factor, :experience_mod, "
        " :schedule_mod, :discount_pct, :surcharge_amt, :tax_amt, "
        " :total_premium, 'AN', :calc_date, 'PREMBAT')"
    )

    with engine.connect() as conn:
        rows = conn.execute(select_sql, {"policy_type": policy_type}).fetchall()
        result.policies_read = len(rows)
        log.info("Partition %s: %d policies to process", policy_type, len(rows))

        batch: list[dict[str, Any]] = []
        for row in rows:
            policy_number = row[0]
            p_type = row[1].strip() if row[1] else policy_type
            total_prem = Decimal(str(row[2])) if row[2] is not None else Decimal("0")
            deductible = Decimal(str(row[3])) if row[3] is not None else Decimal("0")
            cov_limit = Decimal(str(row[4])) if row[4] is not None else Decimal("0")
            eff_date = row[5]
            exp_date = row[6]
            territory = row[7].strip() if row[7] else None
            cls_code = row[8].strip() if row[8] else None

            try:
                calc = calculate_premium(
                    p_type, total_prem, deductible, cov_limit,
                    territory, cls_code, factors,
                )
                batch.append({
                    "policy_number": policy_number,
                    "eff_date": eff_date,
                    "exp_date": exp_date,
                    "calc_date": calc_date,
                    **calc,
                })
            except Exception as exc:
                result.policies_error += 1
                result.errors.append(f"{policy_number}: {exc}")
                log.error("Error calculating premium for %s: %s", policy_number, exc)
                continue

            if len(batch) >= batch_size:
                try:
                    conn.execute(insert_sql, batch)
                    conn.commit()
                    result.policies_updated += len(batch)
                except Exception as exc:
                    conn.rollback()
                    result.policies_error += len(batch)
                    result.errors.append(f"Batch insert error: {exc}")
                    log.error("Batch insert failed for %s: %s", policy_type, exc)
                batch = []

        if batch:
            try:
                conn.execute(insert_sql, batch)
                conn.commit()
                result.policies_updated += len(batch)
            except Exception as exc:
                conn.rollback()
                result.policies_error += len(batch)
                result.errors.append(f"Batch insert error: {exc}")
                log.error("Final batch insert failed for %s: %s", policy_type, exc)

    log.info(
        "Partition %s complete: read=%d updated=%d errors=%d",
        policy_type, result.policies_read, result.policies_updated, result.policies_error,
    )
    return result


def run_premium_batch(
    db_url: str | None = None,
    max_workers: int | None = None,
    batch_size: int | None = None,
    policy_types: list[str] | None = None,
) -> list[PartitionResult]:
    """Run the parallelized premium batch across all policy_type partitions.

    Returns a list of PartitionResult, one per policy_type.
    """
    url = db_url or DB_URL
    workers = max_workers or MAX_WORKERS
    chunk = batch_size or BATCH_SIZE
    types = policy_types or POLICY_TYPES
    calc_date = date.today().isoformat()

    engine = _build_engine(url)
    factors = load_rating_factors(engine)
    engine.dispose()

    factors_dict: dict[str, Any] = {
        "territory": {k: str(v) for k, v in factors.territory.items()},
        "class_code": {k: str(v) for k, v in factors.class_code.items()},
    }

    results: list[PartitionResult] = []

    logger.info(
        "Starting premium batch: %d partitions, %d workers, batch_size=%d",
        len(types), workers, chunk,
    )

    with ProcessPoolExecutor(max_workers=workers) as executor:
        futures = {
            executor.submit(
                _process_partition, pt, url, factors_dict, chunk, calc_date,
            ): pt
            for pt in types
        }
        for future in as_completed(futures):
            pt = futures[future]
            try:
                result = future.result()
                results.append(result)
            except Exception as exc:
                logger.error("Partition %s raised: %s", pt, exc)
                results.append(PartitionResult(
                    policy_type=pt,
                    policies_error=1,
                    errors=[str(exc)],
                ))

    total_read = sum(r.policies_read for r in results)
    total_updated = sum(r.policies_updated for r in results)
    total_errors = sum(r.policies_error for r in results)

    logger.info("=" * 60)
    logger.info("PREMIUM BATCH SUMMARY")
    logger.info("  Total policies read:    %d", total_read)
    logger.info("  Total policies updated: %d", total_updated)
    logger.info("  Total errors:           %d", total_errors)
    for r in results:
        logger.info(
            "  Partition %-3s: read=%d updated=%d errors=%d",
            r.policy_type, r.policies_read, r.policies_updated, r.policies_error,
        )
    logger.info("=" * 60)

    return results


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(name)s %(levelname)s %(message)s",
    )
    run_premium_batch()
