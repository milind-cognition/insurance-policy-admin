"""Monthly Exposure Extract DAG.

Replaces:
  - jcl/MONTHLY-EXPOSURE.jcl  (CA-7 Job# PAS0200)

Schedule: 1st of month at 02:00 US/Eastern (was CA-7 monthly 02:00 EST).

Aggregates exposure data by line of business, branch, coverage type,
territory, and class code for actuarial analysis (SAS loss reserving
and pricing models).  Uploads the CSV to the actuarial server via SFTP.
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
SFTP_ACTUARIAL_CONN_ID = "sftp_actuarial_server"
ACTUARIAL_DATA_DIR = "/actuarial/data"


def _extract_exposure(**context):
    """Run aggregate exposure query and write CSV.

    Runs the SQL from MONTHLY-EXPOSURE.jcl lines 40-57.  Output is
    consumed by actuarial SAS programs for loss reserving / pricing.
    """
    ds = context["ds"]
    sql = (_SQL_DIR / "extract_exposure.sql").read_text()

    hook = DbApiHook.get_hook(DB_CONN_ID)
    conn = hook.get_conn()
    cursor = conn.cursor()

    extract_dir = Path(EXTRACT_DIR)
    extract_dir.mkdir(parents=True, exist_ok=True)
    out_path = extract_dir / f"exposure_monthly_{ds}.csv"

    try:
        cursor.execute(sql)
        columns = [desc[0] for desc in cursor.description]
        rows = cursor.fetchall()
    finally:
        cursor.close()
        conn.close()

    with open(out_path, "w", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(columns)
        writer.writerows(rows)

    log.info("Extracted %d exposure rows to %s", len(rows), out_path)
    context["ti"].xcom_push(key="exposure_path", value=str(out_path))


# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

default_args = {
    "owner": "pas-batch",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="monthly_exposure",
    description="Monthly exposure extract for actuarial (replaces MONTHLY-EXPOSURE.jcl)",
    schedule="0 2 1 * *",
    start_date=pendulum.datetime(2024, 1, 1, tz="US/Eastern"),
    catchup=False,
    default_args=default_args,
    tags=["pas", "exposure", "actuarial"],
) as dag:

    extract_exposure = PythonOperator(
        task_id="extract_exposure",
        python_callable=_extract_exposure,
    )

    sftp_to_actuarial = SFTPUploadOperator(
        task_id="sftp_to_actuarial",
        sftp_conn_id=SFTP_ACTUARIAL_CONN_ID,
        local_paths=[
            f"{EXTRACT_DIR}/exposure_monthly_{{{{ ds }}}}.csv",
        ],
        remote_paths=[
            f"{ACTUARIAL_DATA_DIR}/exposure_monthly.csv",
        ],
    )

    extract_exposure >> sftp_to_actuarial
