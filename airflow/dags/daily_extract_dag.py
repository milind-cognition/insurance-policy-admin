"""Daily Policy Extract DAG.

Replaces:
  - jcl/DAILY-EXTRACT.jcl  (CA-7 Job# PAS0100)

Schedule: Daily at 23:00 US/Eastern (was CA-7 daily 23:00 EST).

Extracts active/pending policy and coverage data, then uploads to the
Claims Engine via SFTP (replacing the plaintext FTP step).
"""

from __future__ import annotations

import csv
import logging
import os
from datetime import timedelta
from pathlib import Path

import pendulum
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.providers.common.sql.hooks.sql import DbApiHook

from operators.sftp_upload_operator import SFTPUploadOperator

log = logging.getLogger(__name__)

_SQL_DIR = Path(__file__).resolve().parent.parent / "sql"

DB_CONN_ID = "pas_database"
EXTRACT_DIR = os.environ.get("PAS_EXTRACT_DIR", "/data/extracts")
SFTP_CLAIMS_CONN_ID = "sftp_claims_server"
CLAIMS_IMPORT_DIR = "/claims/import"


def _extract_policies(**context):
    """Extract recently updated active/pending policies to CSV.

    Runs the SQL from DAILY-EXTRACT.jcl lines 47-55.  Writes to a
    delimited file at ``/data/extracts/policy_extract_{ds}.dat``.
    """
    ds = context["ds"]
    sql = (_SQL_DIR / "extract_policies.sql").read_text()

    hook = DbApiHook.get_hook(DB_CONN_ID)
    conn = hook.get_conn()
    cursor = conn.cursor()

    extract_dir = Path(EXTRACT_DIR)
    extract_dir.mkdir(parents=True, exist_ok=True)
    out_path = extract_dir / f"policy_extract_{ds}.dat"

    try:
        cursor.execute(sql)
        columns = [desc[0] for desc in cursor.description]
        rows = cursor.fetchall()
    finally:
        cursor.close()
        conn.close()

    with open(out_path, "w", newline="") as fh:
        writer = csv.writer(fh, delimiter="|")
        writer.writerow(columns)
        writer.writerows(rows)

    log.info("Extracted %d policy rows to %s", len(rows), out_path)
    context["ti"].xcom_push(key="policy_extract_path", value=str(out_path))


def _extract_coverages(**context):
    """Extract active coverage records to CSV.

    Runs the SQL from DAILY-EXTRACT.jcl lines 75-80.  Writes to
    ``/data/extracts/coverage_extract_{ds}.dat``.
    """
    ds = context["ds"]
    sql = (_SQL_DIR / "extract_coverages.sql").read_text()

    hook = DbApiHook.get_hook(DB_CONN_ID)
    conn = hook.get_conn()
    cursor = conn.cursor()

    extract_dir = Path(EXTRACT_DIR)
    extract_dir.mkdir(parents=True, exist_ok=True)
    out_path = extract_dir / f"coverage_extract_{ds}.dat"

    try:
        cursor.execute(sql)
        columns = [desc[0] for desc in cursor.description]
        rows = cursor.fetchall()
    finally:
        cursor.close()
        conn.close()

    with open(out_path, "w", newline="") as fh:
        writer = csv.writer(fh, delimiter="|")
        writer.writerow(columns)
        writer.writerows(rows)

    log.info("Extracted %d coverage rows to %s", len(rows), out_path)
    context["ti"].xcom_push(key="coverage_extract_path", value=str(out_path))


# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

default_args = {
    "owner": "pas-batch",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="daily_extract",
    description="Nightly policy/coverage extract to Claims Engine (replaces DAILY-EXTRACT.jcl)",
    schedule="0 23 * * *",
    start_date=pendulum.datetime(2024, 1, 1, tz="US/Eastern"),
    catchup=False,
    default_args=default_args,
    tags=["pas", "extract", "claims"],
) as dag:

    extract_policies = PythonOperator(
        task_id="extract_policies",
        python_callable=_extract_policies,
    )

    extract_coverages = PythonOperator(
        task_id="extract_coverages",
        python_callable=_extract_coverages,
    )

    sftp_to_claims = SFTPUploadOperator(
        task_id="sftp_to_claims",
        sftp_conn_id=SFTP_CLAIMS_CONN_ID,
        local_paths=[
            f"{EXTRACT_DIR}/policy_extract_{{{{ ds }}}}.dat",
            f"{EXTRACT_DIR}/coverage_extract_{{{{ ds }}}}.dat",
        ],
        remote_paths=[
            f"{CLAIMS_IMPORT_DIR}/policy_extract.dat",
            f"{CLAIMS_IMPORT_DIR}/coverage_extract.dat",
        ],
    )

    [extract_policies, extract_coverages] >> sftp_to_claims
