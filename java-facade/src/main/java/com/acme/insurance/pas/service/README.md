# Service Layer — PREMBAT Migration

## Overview

The `PremiumBatchService` is a Java translation of the COBOL batch program **PREMBAT**
(Premium Calculation Batch). The original program ran via JCL on the mainframe to
calculate premiums for all active policies and write them to the `PREMIUMS` table.

## REST Endpoint

```
POST /api/v1/policies/premium-batch
```

No request body required. Returns a JSON response with processing counts:

```json
{
  "policiesRead": 3,
  "policiesUpdated": 3,
  "policiesError": 0,
  "message": "PREMBAT completed successfully"
}
```

## COBOL Paragraph → Java Method Mapping

| COBOL Paragraph         | Java Method / Action                          |
|-------------------------|-----------------------------------------------|
| 0000-MAIN-LOGIC         | `calculatePremiums()` orchestrator            |
| 1000-INITIALIZE         | Counter initialization, `new Date()`          |
| 2000-LOAD-RATING-TABLES | Skipped (uses 1.0 defaults, matching COBOL)   |
| 3000-PROCESS-POLICIES   | `findActivePolicies()` + loop                 |
| 3100-FETCH-POLICY       | Loop iteration, `policiesRead++`              |
| 3200-CALCULATE-PREMIUM  | `getBaseRate()`, tax/surcharge calculation     |
| 3300-WRITE-PREMIUM      | `insertPremium()`, error handling              |
| 4000-WRITE-SUMMARY      | Build and return `PremiumBatchResponse`        |

## Business Rules

- **Base rates** by policy type: AUT=$850, HOM=$1,200, COM/CGL=$5,000, LIF=$400, HLT=$3,500, OTHER=$1,000
- **Rating factors**: territory, class, experience, schedule all default to 1.0000
- **Tax rate**: 3.5% of base rate
- **Surcharge**: $25.00 flat per policy
- **Total premium**: base + tax + surcharge
- **Coverage sequence**: always 1
- **Installment code**: 'AN' (annual)
- **Calc by**: 'PREMBAT'

## Technical Notes

- Java 8, Spring Boot 1.5.22, JdbcTemplate (no JPA)
- INSERT uses the composite PK (POLICY_NUMBER, COVERAGE_SEQ, TERM_EFFECTIVE_DATE)
- Duplicate inserts are caught via `DataAccessException` and counted as errors
- All monetary calculations use `BigDecimal` to match COBOL COMP-3 precision
