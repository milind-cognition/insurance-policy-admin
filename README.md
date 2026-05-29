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

### Premium Batch Service (Added 2025)
- **Language:** Java 21
- **Framework:** Spring Boot 3.3 / Spring Batch 5
- **Purpose:** Replaces COBOL `PREMBAT` program and `PREMIUM-BATCH.jcl` job
- **Deployment:** Docker container (same infrastructure as REST facade)
- **Source:** `java-batch/`

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

## Java Batch Module (`java-batch/`) — Premium Calculation Service

This module is a complete rewrite of the COBOL `PREMBAT` batch program (`cobol/programs/PREMBAT.cbl`) and the `PREMIUM-BATCH.jcl` job as a modern Java 21 / Spring Boot 3.3 / Spring Batch 5 application. It calculates premiums for all active policies and writes results to the `PREMIUMS` table.

### What It Replaces

The COBOL `PREMBAT` program (approximately 250 lines) runs nightly at 01:00 via `PREMIUM-BATCH.jcl`. It:
1. Opens a cursor on `POLICIES WHERE POLICY_STATUS = 'AC'` (paragraph `3000-PROCESS-POLICIES`, lines 118-131)
2. Loads territory rating factors from `TERRITORY_FACTORS` (paragraph `2000-LOAD-RATING-TABLES`, lines 102-115 — **stubbed**, always returned 1.0)
3. Calculates premiums using hardcoded base rates per policy type (paragraph `3200-CALCULATE-PREMIUM`, lines 149-188)
4. Writes results to `PREMIUMS` (paragraph `3300-WRITE-PREMIUM-RECORD`, lines 202-218)
5. Produces a summary report (paragraph `4000-WRITE-SUMMARY`, lines 234-250)

The COBOL version processes policies **one at a time** in a single thread, taking approximately **4 hours** for the full book of business.

### Architecture

```
java-batch/
├── pom.xml                         # Maven build (Spring Boot 3.3, Java 21)
├── Dockerfile                      # Container image (eclipse-temurin:21-jre)
└── src/main/java/com/acme/insurance/pas/batch/
    ├── PremiumBatchApplication.java    # Spring Boot entry point
    ├── config/
    │   └── BatchConfig.java            # Job definition: reader → processor → writer
    ├── model/
    │   ├── Policy.java                 # Maps POLICIES + COVERAGES.RATING_TERRITORY
    │   ├── PremiumRecord.java          # Maps PREMIUMS table columns
    │   └── TerritoryFactor.java        # Maps TERRITORY_FACTORS table
    ├── repository/
    │   ├── PolicyRepository.java       # SQL query + RowMapper for active policies
    │   ├── PremiumRepository.java      # Upsert to PREMIUMS table
    │   └── TerritoryFactorRepository.java  # Territory factor lookup
    ├── service/
    │   └── PremiumCalculationService.java  # Core business logic (ported from COBOL)
    └── writer/
        └── PremiumReportWriter.java    # Summary report (policies read/updated/errors)
```

### Component-to-COBOL Mapping

| Java Component | COBOL Equivalent | Description |
|----------------|-----------------|-------------|
| `BatchConfig.java` | `0000-MAIN-LOGIC` + JCL | Job orchestration: single step with chunk-based reader → processor → writer |
| `PolicyRepository.java` | `3000-PROCESS-POLICIES` (lines 118-131) | `SELECT` active policies, now with `LEFT JOIN COVERAGES` for territory codes |
| `PremiumCalculationService.java` | `3200-CALCULATE-PREMIUM` (lines 149-188) | Core formula: `(base × terrFactor) + ((base × terrFactor) × taxRate) + surcharge` |
| `PremiumRepository.java` | `3300-WRITE-PREMIUM-RECORD` (lines 202-218) | Writes to `PREMIUMS` using idempotent upsert (UPDATE first, INSERT if new) |
| `TerritoryFactorRepository.java` | `2000-LOAD-RATING-TABLES` (lines 102-115) | Loads territory factors from DB (COBOL had this **stubbed** — always returned 1.0) |
| `PremiumReportWriter.java` | `4000-WRITE-SUMMARY` (lines 234-250) | Logs summary: total policies read, updated, errors |
| `Policy.java` | `POLICY-RECORD.cpy` | Policy model with `ratingTerritory` from `COVERAGES.RATING_TERRITORY` |
| `PremiumRecord.java` | `PREMIUM-RECORD.cpy` | Premium calculation result model |
| `TerritoryFactor.java` | `WS-TERRITORY-TABLE` | Territory rating factor model |

### Premium Calculation Formula

Ported from COBOL paragraph `3200-CALCULATE-PREMIUM`:

```
baseRate        = lookup by policy type (configurable in application.yml)
territoryFactor = lookup from TERRITORY_FACTORS using COVERAGES.RATING_TERRITORY (default: 1.0)
classFactor     = 1.0 (placeholder, same as COBOL)
experienceMod   = 1.0 (placeholder, same as COBOL)

modifiedPremium = baseRate × territoryFactor × classFactor × experienceMod
taxAmount       = modifiedPremium × taxRate (default: 3.5%)
surchargeAmount = flat surcharge (default: $25.00)

totalPremium    = modifiedPremium + taxAmount + surchargeAmount
```

### Key Improvements over COBOL

| Area | COBOL (`PREMBAT`) | Java (`java-batch`) |
|------|-------------------|---------------------|
| **Base rates** | Hardcoded in `EVALUATE` block (lines 151-164) | Externalized in `application.yml` |
| **Tax rate** | Hardcoded `0.0350` (line 175) | Configurable via `premium.tax-rate` |
| **Surcharge** | Hardcoded `25.00` (line 180) | Configurable via `premium.surcharge` |
| **Territory factors** | Stubbed to always return 1.0 | Loaded from `TERRITORY_FACTORS` table via `COVERAGES.RATING_TERRITORY` |
| **Processing** | Single-threaded, row-by-row | Multi-threaded with configurable `TaskExecutor` and chunk size |
| **Re-runs** | Would fail with duplicate key | Idempotent upsert (UPDATE then INSERT) |
| **Date handling** | Integer arithmetic (leap year bugs) | `java.time.LocalDate` |
| **Thread safety** | N/A (single-threaded) | `SynchronizedItemStreamReader` wraps cursor reader |

### Configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:db2://localhost:50000/ACMEINS
    driver-class-name: com.ibm.db2.jcc.DB2Driver
  batch:
    job:
      enabled: true
    jdbc:
      initialize-schema: always

premium:
  tax-rate: 0.0350          # 3.5% — override for state-specific rates
  surcharge: 25.00           # Flat regulatory surcharge
  default-base-rate: 1000.00 # Fallback for unknown policy types
  chunk-size: 100            # Records per commit (COBOL: 1)
  thread-pool-size: 4        # Parallel threads (COBOL: 1)
  base-rates:                # Per-policy-type base rates
    AUT: 850.00
    HOM: 1200.00
    COM: 5000.00
    LIF: 400.00
    HLT: 3500.00
```

### Data Flow

```
POLICIES table ──LEFT JOIN──▶ COVERAGES table (SEQUENCE_NUM = 1)
       │                              │
       │ policy fields                │ RATING_TERRITORY
       ▼                              ▼
  PolicyRepository.SELECT_ACTIVE_POLICIES
       │
       ▼
  PremiumCalculationService.calculate()
       │
       │ looks up TERRITORY_FACTORS by RATING_TERRITORY
       │ applies formula: (base × terrFactor) + tax + surcharge
       ▼
  PremiumRepository.upsert()  ──▶  PREMIUMS table
       │
       ▼
  PremiumReportWriter  ──▶  Application logs (summary report)
```

### DB2 Tables Used

| Table | Access | Purpose |
|-------|--------|---------|
| `POLICIES` | Read | Active policies (`POLICY_STATUS = 'AC'`) |
| `COVERAGES` | Read | Territory code (`RATING_TERRITORY`) for primary coverage (`SEQUENCE_NUM = 1`) |
| `TERRITORY_FACTORS` | Read | Rating factor lookup by territory code and effective date |
| `PREMIUMS` | Write (upsert) | Calculated premium records |
| Spring Batch metadata tables | Read/Write | Job execution history (`BATCH_JOB_*`, `BATCH_STEP_*`) |

### Build & Run

```bash
# Build (requires Java 21)
cd java-batch
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn package -DskipTests

# Run against DB2
java -jar target/pas-batch-1.0.0.jar \
  --spring.datasource.url=jdbc:db2://host:50000/ACMEINS \
  --spring.datasource.username=USER \
  --spring.datasource.password=PASS

# Run via Docker
docker build -t pas-batch .
docker run \
  -e SPRING_DATASOURCE_URL=jdbc:db2://host:50000/ACMEINS \
  -e SPRING_DATASOURCE_USERNAME=USER \
  -e SPRING_DATASOURCE_PASSWORD=PASS \
  pas-batch

# Override configuration at runtime
java -jar target/pas-batch-1.0.0.jar \
  --premium.tax-rate=0.04 \
  --premium.chunk-size=200 \
  --premium.thread-pool-size=8
```

### Testing

```bash
# Run all tests (18 unit + integration tests against H2 in-memory DB)
cd java-batch
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test

# Run a specific test class
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test -Dtest=PremiumBatchJobTest
```

Tests use H2 in-memory database with `MODE=DB2` — no external database required.

| Test Class | Count | What It Verifies |
|------------|-------|-----------------|
| `PremiumCalculationServiceTest` | 15 | Base rates for all 5 policy types, unknown type default, tax/surcharge math, territory factor application, record metadata |
| `PremiumBatchJobTest` | 3 | Full batch job against H2: correct record count, premium amounts with territory factors, cancelled policy exclusion |

**Expected test output:** `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`

### Idempotent Re-runs

The batch job supports re-running via Spring Batch's `RunIdIncrementer`. The `PremiumRepository` uses an upsert pattern (UPDATE first, INSERT if no existing record) so re-running the job for the same policies will update existing records rather than failing with duplicate key violations. This is safe for:
- Nightly scheduled runs that may overlap
- Manual re-runs after configuration changes
- Recovery from partial failures

## Known Limitations

1. **No real-time API for writes** - All mutations require CICS terminal or MQ message
2. ~~**Hardcoded rating factors**~~ - **Resolved in `java-batch`**: base rates, tax rate, and surcharge are now configurable in `application.yml`; territory factors loaded from `TERRITORY_FACTORS` table
3. **Leap year bug** - POLRNW renewal date calculation does not handle leap years correctly
4. **No state-specific rate caps** - Using flat 15% rate increase cap instead of state-specific regulatory limits
5. **Java facade has no authentication** - Relies on network segmentation (internal VPN only)
6. ~~**Single-threaded batch**~~ - **Resolved in `java-batch`**: configurable multi-threaded `TaskExecutor` with chunk-based processing (COBOL `PREMBAT` still single-threaded if used)
7. **FTP file transfer** - No encryption on daily extract files (regulatory risk)

## Source Control

- **COBOL/JCL:** IBM Rational Team Concert (RTC) on z/OS
- **Java facade:** SVN (svn://svn.acme.local/pas-facade/trunk)
- **No CI/CD pipeline** - Manual compilation and deployment

## Team Contacts

- **Mainframe Team:** mainframe-ops@acme.com
- **DBA Team:** db2-admin@acme.com
- **Java Facade:** t.nguyen@acme.com
