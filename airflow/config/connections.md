# Required Airflow Connections

Configure these connections in the Airflow UI (**Admin → Connections**) or via
environment variables before enabling the DAGs.

## `pas_database`

Database connection to the Policy Administration System (PAS) database.

| Field    | Value (example)                  |
|----------|----------------------------------|
| Conn Id  | `pas_database`                   |
| Conn Type| Postgres (or JDBC for DB2)       |
| Host     | `pasdb.acme.local`               |
| Schema   | `ACMEINS`                        |
| Login    | `pas_batch`                      |
| Password | *(vault/secret)*                 |
| Port     | `5432` (Postgres) / `50000` (DB2)|

For the local docker-compose dev environment this is pre-configured via the
`AIRFLOW_CONN_PAS_DATABASE` environment variable pointing at the
`postgres-pas` container.

If the database remains **DB2**, change the Conn Type to **JDBC** and set the
extra field:

```json
{
  "driver_class": "com.ibm.db2.jcc.DB2Driver",
  "driver_path": "/opt/airflow/lib/db2jcc4.jar"
}
```

---

## `sftp_claims_server`

SFTP connection to the Claims Engine import server.  Replaces the plaintext
FTP step in `DAILY-EXTRACT.jcl` (lines 86-96).

| Field    | Value (example)                  |
|----------|----------------------------------|
| Conn Id  | `sftp_claims_server`             |
| Conn Type| SFTP                             |
| Host     | `claimsrv.acme.local`            |
| Login    | `claimsftp`                      |
| Port     | `22`                             |
| Extra    | `{"key_file": "/opt/airflow/.ssh/claims_rsa"}` |

Use **SSH key authentication** instead of passwords to comply with the
regulatory requirement noted in the repo README (line 76).

---

## `sftp_actuarial_server`

SFTP connection to the Actuarial shared drive.  Replaces the FTP step in
`MONTHLY-EXPOSURE.jcl` (lines 63-69).

| Field    | Value (example)                  |
|----------|----------------------------------|
| Conn Id  | `sftp_actuarial_server`          |
| Conn Type| SFTP                             |
| Host     | `actuarial.acme.local`           |
| Login    | `actuarialftp`                   |
| Port     | `22`                             |
| Extra    | `{"key_file": "/opt/airflow/.ssh/actuarial_rsa"}` |
