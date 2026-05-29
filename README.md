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

### Java Batch — Premium Calculation (Replaces COBOL PREMBAT)
- **Language:** Java 17
- **Framework:** Spring Boot 3.3 / Spring Batch 5
- **Purpose:** Recalculates premiums for all active policies (replaces `PREMBAT` program and `PREMIUM-BATCH.jcl`)
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

## Java Batch Module (`java-batch/`)

The `java-batch/` module is a Spring Boot 3.3 / Spring Batch 5 application that replaces the COBOL `PREMBAT` batch program and `PREMIUM-BATCH.jcl` job.

### Data Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring Batch Job: premiumBatchJob               │
│                                                                     │
│  ┌──────────────┐    ┌───────────────────┐    ┌──────────────────┐  │
│  │  ItemReader   │    │  ItemProcessor     │    │  ItemWriter      │  │
│  │              │    │                   │    │                  │  │
│  │  POLICIES    │    │  PremiumCalc-     │    │  INSERT INTO     │  │
│  │  LEFT JOIN   ├───►│  ulationService   ├───►│  PREMIUMS        │  │
│  │  COVERAGES   │    │                   │    │  (batch of 100)  │  │
│  │              │    │  ┌─────────────┐  │    │                  │  │
│  │  WHERE       │    │  │ base rate   │  │    └──────────────────┘  │
│  │  STATUS='AC' │    │  │ (from yml)  │  │                         │
│  └──────────────┘    │  ├─────────────┤  │    ┌──────────────────┐  │
│                      │  │ territory   │  │    │  StepListener    │  │
│  ┌──────────────┐    │  │ factor      │◄─┼────│                  │  │
│  │ TERRITORY_   │    │  │ (from DB2)  │  │    │  PremiumReport-  │  │
│  │ FACTORS      ├───►│  ├─────────────┤  │    │  Writer          │  │
│  │              │    │  │ + tax 3.5%  │  │    │                  │  │
│  └──────────────┘    │  │ + surcharge │  │    │  Logs summary:   │  │
│                      │  │   $25.00    │  │    │  read / updated  │  │
│  ┌──────────────┐    │  └─────────────┘  │    │  / errors        │  │
│  │ application  │    │                   │    └──────────────────┘  │
│  │ .yml         ├───►│  = finalPremium   │                         │
│  │ (rates,tax)  │    │                   │                         │
│  └──────────────┘    └───────────────────┘                         │
│                                                                     │
│  Chunks of 100 policies ──► multi-threaded (configurable)           │
└─────────────────────────────────────────────────────────────────────┘
```

### Key improvements over the COBOL version

1. **Externalized configuration** — base rates, tax rate, and surcharge are in `application.yml` instead of hardcoded in COBOL
2. **Multi-threaded processing** — configurable `TaskExecutor` parallelizes chunk processing (addresses the ~4-hour sequential runtime)
3. **Chunk-based commits** — batch commits in configurable chunks (default 100) instead of row-by-row
4. **Territory factor loading** — loads and applies territory factors from DB2 (COBOL version had this stubbed)
5. **Modern date handling** — uses `java.time.LocalDate` instead of integer date arithmetic

### Build & Run

```bash
# Build (requires Java 17+)
cd java-batch
mvn package

# Run against DB2
java -jar target/pas-batch-1.0.0.jar

# Run with Docker
docker build -t pas-batch .
docker run -e DB2_USERNAME=user -e DB2_PASSWORD=pass pas-batch
```

### Configuration (`application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `premium.tax-rate` | `0.0350` | Tax rate applied to modified premium |
| `premium.surcharge` | `25.00` | Flat regulatory surcharge per policy |
| `premium.base-rates.*` | varies | Base rate per policy type (AUT, HOM, COM, LIF, HLT) |
| `premium.chunk-size` | `100` | Number of policies per batch commit |
| `premium.concurrency` | `4` | Number of parallel processing threads |

### Tests

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
