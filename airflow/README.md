# Airflow Batch Jobs — PAS Modernization

This directory contains Apache Airflow DAGs that replace the three JCL batch
jobs and the PREMBAT COBOL program from the Policy Administration System (PAS).

## Migration Mapping

| Legacy Artifact | Airflow DAG | Schedule | Description |
|---|---|---|---|
| `jcl/PREMIUM-BATCH.jcl` + `cobol/programs/PREMBAT.cbl` | `dags/premium_batch_dag.py` | Daily 01:00 EST | Recalculate premiums for all active policies |
| `jcl/DAILY-EXTRACT.jcl` | `dags/daily_extract_dag.py` | Daily 23:00 EST | Extract policy/coverage data, SFTP to Claims Engine |
| `jcl/MONTHLY-EXPOSURE.jcl` | `dags/monthly_exposure_dag.py` | 1st of month 02:00 EST | Aggregate exposure data, SFTP to Actuarial |

**Scheduler:** CA-7 → Apache Airflow  
**File transfer:** FTP (plaintext) → SFTP (SSH key auth)  
**Premium logic:** COBOL PREMBAT (`COMP-3` packed decimal) → Python `decimal.Decimal`

## Premium Calculation Logic (ported from PREMBAT.cbl)

The premium calculation in `premium_batch_dag.py` mirrors
`cobol/programs/PREMBAT.cbl` paragraph `3200-CALCULATE-PREMIUM` (lines
149-188):

```
modified = base_rate × territory_factor × class_factor × experience_mod
tax      = modified × 0.035   (rounded HALF_UP to 2 dp)
surcharge = $25.00            (flat regulatory surcharge)
final    = modified + tax + surcharge
```

Base rates by policy type:

| Code | Type | Base Rate |
|------|------|-----------|
| AUT  | Auto | $850.00 |
| HOM  | Home | $1,200.00 |
| COM  | Commercial | $5,000.00 |
| LIF  | Life | $400.00 |
| HLT  | Health | $3,500.00 |
| *other* | Default | $1,000.00 |

All financial arithmetic uses `decimal.Decimal` (never Python floats) to match
the COBOL `COMP-3` packed-decimal behavior.  The `CALC_BY` field is set to
`'AIRFLOW'` instead of `'PREMBAT'` to distinguish records written by the new
system.

### Known Differences from Original

- **Territory and class factors** currently default to `1.0000` in both the old
  COBOL code and the new Python code.  The COBOL program loads from
  `TERRITORY_FACTORS` / class tables but applies a hardcoded `1.00` multiplier
  (see PREMBAT.cbl lines 167-176).  The Python DAG loads real territory factors
  from the database; class and experience factors remain `1.0000` (marked with
  `TODO` comments).
- **Chunked processing:** The COBOL program processes one row at a time with a
  single commit at the end.  The Airflow DAG processes in chunks of 1,000 rows
  with intermediate commits for partial restartability.
- **Report format:** The COBOL program writes a fixed-width 132-character report
  file.  The Airflow DAG writes a plain-text summary file (the per-policy detail
  lines are replaced by database-queryable records).

## Running Locally with Docker Compose

```bash
cd airflow/
docker compose up --build -d
```

This starts:

| Service | Purpose | Port |
|---|---|---|
| `airflow-webserver` | Airflow UI | http://localhost:8080 |
| `airflow-scheduler` | DAG scheduling | — |
| `postgres-airflow` | Airflow metadata DB | 5432 |
| `postgres-pas` | Simulated PAS database | 5433 |

The `postgres-pas` container auto-loads the schema from `sql/ddl/create-tables.sql`.

Default credentials: **admin / admin**.

## Configuring Connections

See [`config/connections.md`](config/connections.md) for the three Airflow
connections that must be configured:

- `pas_database` — PAS DB2/PostgreSQL
- `sftp_claims_server` — Claims Engine SFTP
- `sftp_actuarial_server` — Actuarial SFTP

In the local docker-compose environment, `pas_database` is pre-configured via
the `AIRFLOW_CONN_PAS_DATABASE` environment variable.

## Running Tests

```bash
# Install test dependencies
pip install -r requirements.txt pytest

# Premium calculation unit tests (no Airflow required)
python -m pytest tests/test_premium_calculation.py -v

# DAG validation tests (requires Airflow)
python -m pytest tests/test_daily_extract_dag.py tests/test_monthly_exposure_dag.py -v
```

## Directory Structure

```
airflow/
  dags/
    premium_batch_dag.py        # Replaces PREMIUM-BATCH.jcl + PREMBAT.cbl
    daily_extract_dag.py        # Replaces DAILY-EXTRACT.jcl
    monthly_exposure_dag.py     # Replaces MONTHLY-EXPOSURE.jcl
  plugins/
    operators/
      __init__.py
      sftp_upload_operator.py   # Reusable SFTP upload wrapper
  config/
    connections.md              # Required Airflow connection docs
  sql/
    extract_policies.sql        # Daily extract: policy headers
    extract_coverages.sql       # Daily extract: coverage details
    extract_exposure.sql        # Monthly exposure aggregate query
    select_active_policies.sql  # Premium batch: active policies cursor
    select_territory_factors.sql# Premium batch: territory rating factors
    insert_premium.sql          # Premium batch: INSERT INTO PREMIUMS
  tests/
    test_premium_calculation.py # Premium calc unit tests
    test_daily_extract_dag.py   # DAG structure validation
    test_monthly_exposure_dag.py# DAG structure validation
  requirements.txt
  Dockerfile
  docker-compose.yml            # Local dev environment
  README.md                     # This file
```

## DB2 vs PostgreSQL

The SQL files use PostgreSQL syntax by default (e.g., `CURRENT_DATE - INTERVAL '1 DAY'`).
If the database remains DB2:

1. Replace `apache-airflow-providers-postgres` with `apache-airflow-providers-jdbc`
   and `ibm_db_sa` in `requirements.txt`.
2. Adjust date expressions: `CURRENT_DATE - INTERVAL '1 DAY'` → `CURRENT DATE - 1 DAY`.
3. Update the `pas_database` connection to use a JDBC Conn Type with the DB2
   driver (see `config/connections.md`).
