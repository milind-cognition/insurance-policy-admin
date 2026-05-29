FROM apache/airflow:2.9.3-python3.11

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Copy DAGs, plugins, SQL, and config into the image
COPY dags/        ${AIRFLOW_HOME}/dags/
COPY plugins/     ${AIRFLOW_HOME}/plugins/
COPY sql/         ${AIRFLOW_HOME}/sql/
COPY config/      ${AIRFLOW_HOME}/config/
