"""Tests for the daily extract DAG structure."""

def test_dag_loads():
    """Verify the DAG can be parsed without import errors."""
    from dags.daily_extract_dag import dag

    assert dag.dag_id == "daily_policy_extract"


def test_dag_has_expected_tasks():
    from dags.daily_extract_dag import dag

    task_ids = {t.task_id for t in dag.tasks}
    assert task_ids == {"extract_policies", "extract_coverages", "sftp_upload"}


def test_sftp_upload_depends_on_extracts():
    from dags.daily_extract_dag import dag

    sftp_task = dag.get_task("sftp_upload")
    upstream_ids = {t.task_id for t in sftp_task.upstream_list}
    assert upstream_ids == {"extract_policies", "extract_coverages"}


def test_extract_tasks_have_no_upstream():
    from dags.daily_extract_dag import dag

    for tid in ("extract_policies", "extract_coverages"):
        task = dag.get_task(tid)
        assert len(task.upstream_list) == 0


def test_dag_schedule():
    from dags.daily_extract_dag import dag

    assert dag.schedule_interval == "0 23 * * *"


def test_dag_tags():
    from dags.daily_extract_dag import dag

    assert "pas" in dag.tags
    assert "extract" in dag.tags
