# Policy Administration System (PAS)

## Overview

The Policy Administration System is the **core insurance platform** for Acme Insurance, managing all policy lifecycle operations. Originally built in **1998** on IBM mainframe technology, it remains the system of record for all insurance policies.

**Maturity Level:** 0 (Legacy)

## Architecture

### Core Platform
- **Language:** COBOL 85
- **Runtime:** IBM z/OS 2.5, CICS Transaction Server 5.6
- **Database:** IBM DB2 v12 for z/OS
- **Messaging:** IBM MQ Series 9.2
- **Batch:** z/OS JES2, CA-7 job scheduling

### REST Facade (Added 2022)
- **Language:** Java 8
- **Framework:** Spring Boot 1.5.22
- **Purpose:** Read-only REST API for modern system integration
- **Deployment:** Docker container on Linux VM (not on z/OS)

### Premium Batch (Java) — Replaces COBOL PREMBAT
- **Language:** Java 17
- **Framework:** Spring Boot 3.3 / Spring Batch 5
- **Purpose:** Recalculates premiums for all active policies (replaces `PREMBAT.cbl` and `PREMIUM-BATCH.jcl`)
- **Deployment:** Docker container or standalone JAR

## CICS Transactions

| Transaction | Program | Description |
|-------------|---------|-------------|
| PNEW | POLNEW | Create new policy |
| PRWL | POLRNW | Policy renewal |
| PEND | POLEND | Endorsement processing |
| PQRY | POLQRY | Policy inquiry (read-only) |
| PUWR | UNDWRT | Underwriting decision |

## Batch Jobs

| JCL Job | Schedule | Description |
|---------|----------|-------------|
| DAILY-EXTRACT | Daily 23:00 | Extract policy data for Claims/Broker |
| MONTHLY-EXPOSURE | 1st of month | Exposure data for actuarial models |
| PREMIUM-BATCH | Daily 01:00 | Recalculate premiums (PREMBAT program) |

## DB2 Tables

- `POLICIES` - Policy master (header)
- `COVERAGES` - Coverage/LOB detail
- `PREMIUMS` - Premium calculations
- `ENDORSEMENTS` - Policy changes
- `UNDERWRITING_DECISIONS` - UW decision audit trail
- `POLICY_HOLDERS` - Customer master

## REST API Endpoints (Facade)

```
GET /api/v1/policies/{policyNumber}            - Policy details (JSON)
GET /api/v1/policies/{policyNumber}/coverages   - Coverage list (JSON)
GET /manage/health                              - Health check
```

> **WARNING:** The REST facade is **read-only**. All policy mutations must go through CICS transactions on the mainframe.

## Integration Points

- **Claims Engine:** Nightly flat-file extract (FTP) via DAILY-EXTRACT.jcl
- **Actuarial Models:** Monthly exposure CSV via MONTHLY-EXPOSURE.jcl
- **Broker Portal:** SQL Server linked server to DB2 (read-only ODBC)
- **Document Management:** IBM MQ events trigger document creation in FileNet

## Known Limitations

1. **No real-time API for writes** - All mutations require CICS terminal or MQ message
2. **Hardcoded rating factors** - Base rates and territory factors are hardcoded in PREMBAT; should be table-driven
3. **Leap year bug** - POLRNW renewal date calculation does not handle leap years correctly
4. **No state-specific rate caps** - Using flat 15% rate increase cap instead of state-specific regulatory limits
5. **Java facade has no authentication** - Relies on network segmentation (internal VPN only)
6. **Single-threaded batch** - PREMBAT processes policies sequentially; takes ~4 hours for full book
7. **FTP file transfer** - No encryption on daily extract files (regulatory risk)

## Java Premium Batch Module (`java-batch/`)

This module is the modern Java replacement for the COBOL `PREMBAT` program and the `PREMIUM-BATCH.jcl` batch job. It uses Spring Boot 3.3 and Spring Batch 5 to recalculate premiums for all active policies.

### Key improvements over the COBOL version

1. **Externalized configuration** — base rates, tax rate, and surcharge are in `application.yml` instead of hardcoded in COBOL
2. **Multi-threaded processing** — Spring Batch `TaskExecutor` parallelizes chunks to address the 4-hour runtime bottleneck
3. **Chunk-based processing** — batch commits in configurable chunks (default 100) instead of row-by-row
4. **Proper territory factor loading** — loads and applies territory factors from DB2 (the COBOL version has this stubbed)
5. **Modern date handling** — uses `java.time.LocalDate` instead of integer arithmetic (avoids leap year bugs)

### Build and run

```bash
# Build (requires Java 17+)
cd java-batch
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn clean package

# Run with DB2
java -jar target/pas-premium-batch-1.0.0.jar \
  --spring.datasource.url=jdbc:db2://localhost:50000/ACMEINS \
  --spring.datasource.username=PASFACAD \
  --spring.datasource.password=changeme

# Run with Docker
docker build -t pas-premium-batch .
docker run -e DB2_USERNAME=PASFACAD -e DB2_PASSWORD=changeme pas-premium-batch
```

### Run tests

```bash
cd java-batch
mvn test
```

## Source Control

- **COBOL/JCL:** IBM Rational Team Concert (RTC) on z/OS
- **Java facade:** SVN (svn://svn.acme.local/pas-facade/trunk)
- **No CI/CD pipeline** - Manual compilation and deployment

## Team Contacts

- **Mainframe Team:** mainframe-ops@acme.com
- **DBA Team:** db2-admin@acme.com
- **Java Facade:** t.nguyen@acme.com
