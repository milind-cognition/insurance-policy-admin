"""
Airflow DAG: Daily Policy Extract (replaces DAILY-EXTRACT.jcl)

Migrated from:  jcl/DAILY-EXTRACT.jcl  (JOB POLEXT, CA-7 Job# PAS0100)
Schedule:       Daily at 23:00 EST
Author:         Modernization — original by J. Henderson (1998)

Extracts active/pending policy and coverage data from DB2, writes CSV
files, and transfers them to the Claims server via SFTP (replacing the
legacy unencrypted FTP in STEP030).
"""

from __future__ import annotations

import csv
import logging
import os
from datetime import datetime, timedelta
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.providers.sftp.hooks.sftp import SFTPHook
from airflow.hooks.base import BaseHook

logger = logging.getLogger(__name__)

EXTRACT_DIR = os.getenv("PAS_EXTRACT_DIR", "/tmp/pas_extracts")

DB_CONN_ID = os.getenv("PAS_DB_CONN_ID", "pas_db2")
SFTP_CONN_ID = os.getenv("PAS_SFTP_CONN_ID", "claims_sftp")
SFTP_REMOTE_DIR = os.getenv("PAS_SFTP_REMOTE_DIR", "/claims/import")

POLICY_EXTRACT_SQL = """\
SELECT POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS,
       EFFECTIVE_DATE, EXPIRY_DATE,
       POLICYHOLDER_ID, AGENT_CODE, BRANCH_CODE,
       TOTAL_PREMIUM, DEDUCTIBLE, COVERAGE_LIMIT,
       UW_STATUS, RISK_SCORE
FROM POLICIES
WHERE POLICY_STATUS IN ('AC', 'PN')
  AND LAST_UPDATED >= CURRENT_DATE - INTERVAL '1' DAY
ORDER BY POLICY_NUMBER
"""

COVERAGE_EXTRACT_SQL = """\
SELECT POLICY_NUMBER, SEQUENCE_NUM, COVERAGE_TYPE,
       DESCRIPTION, COVERAGE_LIMIT, DEDUCTIBLE,
       PREMIUM, EFFECTIVE_DATE, EXPIRY_DATE, STATUS
FROM COVERAGES
WHERE STATUS = 'AC'
ORDER BY POLICY_NUMBER, SEQUENCE_NUM
"""

POLICY_COLUMNS = [
    "POLICY_NUMBER", "POLICY_TYPE", "POLICY_STATUS",
    "EFFECTIVE_DATE", "EXPIRY_DATE",
    "POLICYHOLDER_ID", "AGENT_CODE", "BRANCH_CODE",
    "TOTAL_PREMIUM", "DEDUCTIBLE", "COVERAGE_LIMIT",
    "UW_STATUS", "RISK_SCORE",
]

COVERAGE_COLUMNS = [
    "POLICY_NUMBER", "SEQUENCE_NUM", "COVERAGE_TYPE",
    "DESCRIPTION", "COVERAGE_LIMIT", "DEDUCTIBLE",
    "PREMIUM", "EFFECTIVE_DATE", "EXPIRY_DATE", "STATUS",
]


def _ensure_extract_dir() -> Path:
    path = Path(EXTRACT_DIR)
    path.mkdir(parents=True, exist_ok=True)
    return path


def _run_extract(sql: str, columns: list[str], filename: str) -> str:
    """Execute *sql* against DB2 and write results to a CSV file."""
    extract_dir = _ensure_extract_dir()
    filepath = extract_dir / filename

    hook = BaseHook.get_hook(DB_CONN_ID)
    logger.info("Running extract query for %s", filename)
    rows = hook.get_records(sql)
    logger.info("Extracted %d rows for %s", len(rows), filename)

    with open(filepath, "w", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(columns)
        writer.writerows(rows)

    logger.info("Wrote %s (%d bytes)", filepath, filepath.stat().st_size)
    return str(filepath)


def extract_policies(**context):
    """STEP010 replacement — extract policy headers from DB2."""
    path = _run_extract(
        POLICY_EXTRACT_SQL,
        POLICY_COLUMNS,
        "policy_extract.csv",
    )
    context["ti"].xcom_push(key="policy_file", value=path)


def extract_coverages(**context):
    """STEP020 replacement — extract coverage details from DB2."""
    path = _run_extract(
        COVERAGE_EXTRACT_SQL,
        COVERAGE_COLUMNS,
        "coverage_extract.csv",
    )
    context["ti"].xcom_push(key="coverage_file", value=path)


def sftp_upload(**context):
    """STEP030 replacement — upload extracts via SFTP (was unencrypted FTP)."""
    ti = context["ti"]
    policy_file = ti.xcom_pull(task_ids="extract_policies", key="policy_file")
    coverage_file = ti.xcom_pull(task_ids="extract_coverages", key="coverage_file")

    hook = SFTPHook(sftp_conn_id=SFTP_CONN_ID)

    for local_path, remote_name in [
        (policy_file, "policy_extract.csv"),
        (coverage_file, "coverage_extract.csv"),
    ]:
        remote_path = f"{SFTP_REMOTE_DIR}/{remote_name}"
        logger.info("Uploading %s -> %s", local_path, remote_path)
        hook.store_file(remote_path, local_path)
        logger.info("Upload complete: %s", remote_path)


default_args = {
    "owner": "pas-modernization",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 2,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="daily_policy_extract",
    default_args=default_args,
    description=(
        "Nightly extract of active policy & coverage data to Claims "
        "server via SFTP. Replaces DAILY-EXTRACT.jcl (CA-7 PAS0100)."
    ),
    schedule_interval="0 23 * * *",
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["pas", "extract", "claims"],
) as dag:

    t_extract_policies = PythonOperator(
        task_id="extract_policies",
        python_callable=extract_policies,
    )

    t_extract_coverages = PythonOperator(
        task_id="extract_coverages",
        python_callable=extract_coverages,
    )

    t_sftp_upload = PythonOperator(
        task_id="sftp_upload",
        python_callable=sftp_upload,
    )

    [t_extract_policies, t_extract_coverages] >> t_sftp_upload
