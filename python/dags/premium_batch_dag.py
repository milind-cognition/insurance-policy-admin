"""
Airflow DAG: Premium Batch Calculation (replaces PREMIUM-BATCH.jcl)

Migrated from:  jcl/PREMIUM-BATCH.jcl  (JOB PREMBAT, CA-7 Job# PAS0050)
Schedule:       Daily at 01:00 EST
Author:         Modernization — original by M. Kowalski (1998)

Orchestrates the parallelized premium calculation pipeline.  Each
policy_type partition runs as a separate Airflow task so the scheduler
can spread work across the pool and provide per-partition visibility.
"""

from __future__ import annotations

import logging
import os
from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.python import PythonOperator

logger = logging.getLogger(__name__)

DB_URL = os.getenv(
    "PAS_DB_URL",
    "db2+ibm_db://user:pass@localhost:50000/DBPD",
)
BATCH_SIZE = int(os.getenv("PAS_PREMIUM_BATCH_SIZE", "500"))
POLICY_TYPES = ["AUT", "HOM", "COM", "LIF", "HLT"]


def _run_partition(policy_type: str, **context):
    """Run a single policy_type partition of the premium batch."""
    from pipelines.premium_batch import run_premium_batch

    results = run_premium_batch(
        db_url=DB_URL,
        max_workers=1,
        batch_size=BATCH_SIZE,
        policy_types=[policy_type],
    )
    result = results[0]
    context["ti"].xcom_push(key="policies_read", value=result.policies_read)
    context["ti"].xcom_push(key="policies_updated", value=result.policies_updated)
    context["ti"].xcom_push(key="policies_error", value=result.policies_error)

    if result.policies_error > 0:
        logger.warning(
            "Partition %s completed with %d errors",
            policy_type, result.policies_error,
        )


def _summarize(**context):
    """Aggregate results from all partition tasks."""
    ti = context["ti"]
    total_read = 0
    total_updated = 0
    total_errors = 0

    for pt in POLICY_TYPES:
        task_id = f"calc_{pt.lower()}"
        total_read += ti.xcom_pull(task_ids=task_id, key="policies_read") or 0
        total_updated += ti.xcom_pull(task_ids=task_id, key="policies_updated") or 0
        total_errors += ti.xcom_pull(task_ids=task_id, key="policies_error") or 0

    logger.info("=" * 60)
    logger.info("PREMIUM BATCH COMPLETE")
    logger.info("  Total policies read:    %d", total_read)
    logger.info("  Total policies updated: %d", total_updated)
    logger.info("  Total errors:           %d", total_errors)
    logger.info("=" * 60)

    if total_errors > 0:
        logger.warning("Batch completed with %d errors — review logs", total_errors)


default_args = {
    "owner": "pas-modernization",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}

with DAG(
    dag_id="premium_batch_calculation",
    default_args=default_args,
    description=(
        "Parallelized premium recalculation partitioned by policy_type. "
        "Replaces PREMIUM-BATCH.jcl / PREMBAT.cbl (CA-7 PAS0050)."
    ),
    schedule_interval="0 1 * * *",
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["pas", "premium", "batch"],
) as dag:

    partition_tasks = []
    for pt in POLICY_TYPES:
        task = PythonOperator(
            task_id=f"calc_{pt.lower()}",
            python_callable=_run_partition,
            op_kwargs={"policy_type": pt},
        )
        partition_tasks.append(task)

    t_summarize = PythonOperator(
        task_id="summarize",
        python_callable=_summarize,
        trigger_rule="all_done",
    )

    partition_tasks >> t_summarize
