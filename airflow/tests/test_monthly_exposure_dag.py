"""DAG validation tests for monthly_exposure_dag.

Verifies the DAG loads without import errors, has the expected task
dependencies, and uses the correct schedule interval.
"""

import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "dags"))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "plugins"))

from airflow.models import DagBag


class TestMonthlyExposureDag(unittest.TestCase):
    """Validate the monthly_exposure DAG structure."""

    @classmethod
    def setUpClass(cls):
        dags_dir = str(Path(__file__).resolve().parent.parent / "dags")
        plugins_dir = str(Path(__file__).resolve().parent.parent / "plugins")
        if plugins_dir not in sys.path:
            sys.path.insert(0, plugins_dir)
        cls.dagbag = DagBag(dag_folder=dags_dir, include_examples=False)

    def test_dag_loads_without_errors(self):
        self.assertIn("monthly_exposure", self.dagbag.dags)
        self.assertEqual(self.dagbag.import_errors, {})

    def test_schedule_interval(self):
        dag = self.dagbag.dags["monthly_exposure"]
        self.assertEqual(dag.schedule_interval, "0 2 1 * *")

    def test_task_count(self):
        dag = self.dagbag.dags["monthly_exposure"]
        self.assertEqual(len(dag.tasks), 2)

    def test_task_ids(self):
        dag = self.dagbag.dags["monthly_exposure"]
        task_ids = {t.task_id for t in dag.tasks}
        self.assertEqual(task_ids, {"extract_exposure", "sftp_to_actuarial"})

    def test_dependencies(self):
        """extract_exposure >> sftp_to_actuarial."""
        dag = self.dagbag.dags["monthly_exposure"]
        sftp_task = dag.get_task("sftp_to_actuarial")
        upstream_ids = {t.task_id for t in sftp_task.upstream_list}
        self.assertEqual(upstream_ids, {"extract_exposure"})

        extract_task = dag.get_task("extract_exposure")
        self.assertEqual(len(extract_task.upstream_list), 0)


if __name__ == "__main__":
    unittest.main()
