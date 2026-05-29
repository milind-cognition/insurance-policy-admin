"""DAG validation tests for daily_extract_dag.

Verifies the DAG loads without import errors, has the expected task
dependencies, and uses the correct schedule interval.
"""

import unittest
import sys
from pathlib import Path

# Make the dags/ directory importable
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "dags"))
# Make the plugins/ directory importable (for operator imports)
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "plugins"))

# Airflow must be available to parse DAGs
from airflow.models import DagBag


class TestDailyExtractDag(unittest.TestCase):
    """Validate the daily_extract DAG structure."""

    @classmethod
    def setUpClass(cls):
        dags_dir = str(Path(__file__).resolve().parent.parent / "dags")
        plugins_dir = str(Path(__file__).resolve().parent.parent / "plugins")
        if plugins_dir not in sys.path:
            sys.path.insert(0, plugins_dir)
        cls.dagbag = DagBag(dag_folder=dags_dir, include_examples=False)

    def test_dag_loads_without_errors(self):
        self.assertIn("daily_extract", self.dagbag.dags)
        self.assertEqual(self.dagbag.import_errors, {})

    def test_schedule_interval(self):
        dag = self.dagbag.dags["daily_extract"]
        self.assertEqual(dag.schedule_interval, "0 23 * * *")

    def test_task_count(self):
        dag = self.dagbag.dags["daily_extract"]
        self.assertEqual(len(dag.tasks), 3)

    def test_task_ids(self):
        dag = self.dagbag.dags["daily_extract"]
        task_ids = {t.task_id for t in dag.tasks}
        self.assertEqual(
            task_ids,
            {"extract_policies", "extract_coverages", "sftp_to_claims"},
        )

    def test_dependencies(self):
        """extract_policies and extract_coverages run in parallel, then sftp."""
        dag = self.dagbag.dags["daily_extract"]
        sftp_task = dag.get_task("sftp_to_claims")
        upstream_ids = {t.task_id for t in sftp_task.upstream_list}
        self.assertEqual(upstream_ids, {"extract_policies", "extract_coverages"})

        # extract tasks have no upstream
        for tid in ("extract_policies", "extract_coverages"):
            task = dag.get_task(tid)
            self.assertEqual(len(task.upstream_list), 0)


if __name__ == "__main__":
    unittest.main()
