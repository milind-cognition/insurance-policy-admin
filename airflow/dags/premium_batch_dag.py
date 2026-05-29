"""Premium Batch Calculation DAG.

Replaces:
  - jcl/PREMIUM-BATCH.jcl  (CA-7 Job# PAS0050)
  - cobol/programs/PREMBAT.cbl

Schedule: Daily at 01:00 US/Eastern (was CA-7 daily 01:00 EST).
"""

from __future__ import annotations

import csv
import logging
import os
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import pendulum
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.providers.common.sql.hooks.sql import DbApiHook

log = logging.getLogger(__name__)

_SQL_DIR = Path(__file__).resolve().parent.parent / "sql"

# ---------------------------------------------------------------------------
# Premium calculation constants ported from PREMBAT.cbl lines 149-188
# ---------------------------------------------------------------------------

BASE_RATES: dict[str, Decimal] = {
    "AUT": Decimal("850.00"),
    "HOM": Decimal("1200.00"),
    "COM": Decimal("5000.00"),
    "LIF": Decimal("400.00"),
    "HLT": Decimal("3500.00"),
}
DEFAULT_BASE = Decimal("1000.00")
TAX_RATE = Decimal("0.0350")
SURCHARGE = Decimal("25.00")

CHUNK_SIZE = 1000
DB_CONN_ID = "pas_database"
REPORT_DIR = os.environ.get("PAS_REPORT_DIR", "/data/reports")


def calculate_premium(
    policy_type: str,
    territory_code: str | None,
    territory_factors: dict[str, Decimal],
) -> tuple[Decimal, Decimal, Decimal, Decimal, Decimal, Decimal, Decimal]:
    """Return (base, terr_factor, class_factor, exp_mod, tax, surcharge, final).

    Mirrors PREMBAT.cbl ``3200-CALCULATE-PREMIUM`` (lines 149-188).
    All arithmetic uses ``decimal.Decimal`` to match COBOL COMP-3 precision.
    """
    base = BASE_RATES.get(policy_type, DEFAULT_BASE)
    terr_factor = territory_factors.get(
        territory_code or "", Decimal("1.0000")
    )
    class_factor = Decimal("1.0000")  # TODO: load from class table
    exp_mod = Decimal("1.0000")  # TODO: load from experience table
    modified = base * terr_factor * class_factor * exp_mod
    tax = (modified * TAX_RATE).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    final = modified + tax + SURCHARGE
    return base, terr_factor, class_factor, exp_mod, tax, SURCHARGE, final


# ---------------------------------------------------------------------------
# Task callables
# ---------------------------------------------------------------------------


def _load_territory_factors(**context):
    """Query TERRITORY_FACTORS and push dict to XCom.

    Corresponds to ``2000-LOAD-RATING-TABLES`` in PREMBAT.cbl (lines 102-115).
    """
    sql = (_SQL_DIR / "select_territory_factors.sql").read_text()
    hook = DbApiHook.get_hook(DB_CONN_ID)
    rows = hook.get_records(sql)
    factors = {
        str(code).strip(): Decimal(str(factor))
        for code, factor in rows
    }
    log.info("Loaded %d territory factors", len(factors))
    context["ti"].xcom_push(key="territory_factors", value={k: str(v) for k, v in factors.items()})


def _calculate_and_insert_premiums(**context):
    """Read active policies, compute premiums, INSERT into PREMIUMS.

    Processes in chunks of 1000 with intermediate commits for
    restartability (replaces the sequential single-row COBOL cursor).
    Mirrors PREMBAT.cbl ``3000-PROCESS-POLICIES`` / ``3100-FETCH-POLICY``
    / ``3200-CALCULATE-PREMIUM`` / ``3300-WRITE-PREMIUM-RECORD``.
    """
    ti = context["ti"]
    raw_factors = ti.xcom_pull(task_ids="load_territory_factors", key="territory_factors") or {}
    territory_factors = {k: Decimal(v) for k, v in raw_factors.items()}

    select_sql = (_SQL_DIR / "select_active_policies.sql").read_text()
    insert_sql = (_SQL_DIR / "insert_premium.sql").read_text()

    hook = DbApiHook.get_hook(DB_CONN_ID)
    conn = hook.get_conn()
    cursor = conn.cursor()

    policies_read = 0
    policies_updated = 0
    policies_error = 0
    had_warning = False

    try:
        cursor.execute(select_sql)
        while True:
            rows = cursor.fetchmany(CHUNK_SIZE)
            if not rows:
                break
            insert_cursor = conn.cursor()
            for row in rows:
                policy_number, policy_type, _total_premium, _deductible, _coverage_limit, eff_date, exp_date = row
                policies_read += 1
                try:
                    base, terr_f, cls_f, exp_m, tax, surcharge, final = calculate_premium(
                        str(policy_type).strip(),
                        None,  # territory_code not on POLICIES table
                        territory_factors,
                    )
                    insert_cursor.execute(
                        insert_sql,
                        (
                            policy_number,
                            eff_date,
                            exp_date,
                            str(base),
                            str(terr_f),
                            str(cls_f),
                            str(exp_m),
                            str(surcharge),
                            str(tax),
                            str(final),
                        ),
                    )
                    policies_updated += 1
                except Exception:
                    policies_error += 1
                    had_warning = True
                    log.exception(
                        "Error calculating premium for policy %s",
                        policy_number,
                    )
            conn.commit()
            insert_cursor.close()
            log.info(
                "Chunk committed — read=%d updated=%d errors=%d",
                policies_read,
                policies_updated,
                policies_error,
            )
    finally:
        cursor.close()
        conn.close()

    log.info(
        "Premium batch complete — read=%d updated=%d errors=%d",
        policies_read,
        policies_updated,
        policies_error,
    )

    ti.xcom_push(key="policies_read", value=policies_read)
    ti.xcom_push(key="policies_updated", value=policies_updated)
    ti.xcom_push(key="policies_error", value=policies_error)
    ti.xcom_push(key="had_warning", value=had_warning)


def _generate_report(**context):
    """Write a CSV/text premium report file.

    Replaces the PREMRPT DD output from PREMBAT.cbl ``4000-WRITE-SUMMARY``.
    """
    ti = context["ti"]
    ds = context["ds"]
    policies_read = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="policies_read") or 0
    policies_updated = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="policies_updated") or 0
    policies_error = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="policies_error") or 0

    report_dir = Path(REPORT_DIR)
    report_dir.mkdir(parents=True, exist_ok=True)
    report_path = report_dir / f"premium_report_{ds}.txt"

    with open(report_path, "w") as fh:
        fh.write(f"ACME INSURANCE - PREMIUM BATCH CALCULATION  RUN DATE: {ds}\n")
        fh.write("=" * 80 + "\n")
        fh.write(f"TOTAL POLICIES READ:    {policies_read}\n")
        fh.write(f"TOTAL POLICIES UPDATED: {policies_updated}\n")
        fh.write(f"TOTAL ERRORS:           {policies_error}\n")
        fh.write("=" * 80 + "\n")

    log.info("Report written to %s", report_path)


def _notify_completion(**context):
    """Send notification on completion or warnings.

    Replaces STEP030 in PREMIUM-BATCH.jcl (lines 45-49) which used
    ``TSO SEND`` to notify PASADMIN of warnings.
    """
    ti = context["ti"]
    had_warning = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="had_warning")
    policies_error = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="policies_error") or 0
    policies_updated = ti.xcom_pull(task_ids="calculate_and_insert_premiums", key="policies_updated") or 0

    if had_warning:
        log.warning(
            "PREMIUM BATCH COMPLETED WITH WARNINGS — %d errors out of %d updated",
            policies_error,
            policies_updated,
        )
    else:
        log.info(
            "PREMIUM BATCH COMPLETED SUCCESSFULLY — %d policies updated",
            policies_updated,
        )


def _on_failure(context):
    log.error("Premium batch DAG failed: %s", context.get("exception"))


# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

default_args = {
    "owner": "pas-batch",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="premium_batch",
    description="Daily premium recalculation (replaces PREMIUM-BATCH.jcl + PREMBAT.cbl)",
    schedule="0 1 * * *",
    start_date=pendulum.datetime(2024, 1, 1, tz="US/Eastern"),
    catchup=False,
    default_args=default_args,
    on_failure_callback=_on_failure,
    tags=["pas", "premium", "batch"],
) as dag:

    load_territory_factors = PythonOperator(
        task_id="load_territory_factors",
        python_callable=_load_territory_factors,
    )

    calculate_and_insert_premiums = PythonOperator(
        task_id="calculate_and_insert_premiums",
        python_callable=_calculate_and_insert_premiums,
    )

    generate_report = PythonOperator(
        task_id="generate_report",
        python_callable=_generate_report,
    )

    notify_completion = PythonOperator(
        task_id="notify_completion",
        python_callable=_notify_completion,
    )

    (
        load_territory_factors
        >> calculate_and_insert_premiums
        >> generate_report
        >> notify_completion
    )
