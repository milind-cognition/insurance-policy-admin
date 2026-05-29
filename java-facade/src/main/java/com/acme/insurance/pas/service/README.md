# PREMBAT Migration — Premium Batch Calculation

## Source Program
- **COBOL**: `cobol/programs/PREMBAT.cbl`
- **JCL**: `jcl/PREMIUM-BATCH.jcl`
- **Type**: z/OS Batch (no CICS)

## Java Target
- **Service**: `PremiumBatchService.java`
- **Endpoint**: `POST /api/v1/policies/premium-calculation`
- **Controller**: `PolicyController.java`

## COBOL Paragraph → Java Method Mapping

| COBOL Paragraph | Java Equivalent |
|---|---|
| 1000-INITIALIZE | `calculatePremiums()` entry — log start, get current date |
| 2000-LOAD-RATING-TABLES | Territory/class/experience factors default to 1.0 |
| 3000-PROCESS-POLICIES | `findAllActivePolicies()` loop |
| 3100-FETCH-POLICY | Each `Policy` from the active-policies list |
| 3200-CALCULATE-PREMIUM | `getBaseRate()` + tax (3.5%) + surcharge ($25) |
| 3300-WRITE-PREMIUM-RECORD | `insertPremium()` via `PolicyRepository` |
| 4000-WRITE-SUMMARY | `PremiumCalcResponse` with read/updated/error counts |
| 9999-TERMINATE | Return response (error count drives HTTP status in future) |

## Business Rules Preserved
- Base rates hardcoded by policy type: AUT=850, HOM=1200, COM=5000, LIF=400, HLT=3500, OTHER=1000
- Territory factor: 1.00 (default — simplified in current COBOL)
- Class factor: 1.00 (default)
- Experience mod: 1.00 (default)
- Tax rate: 3.50% (`WS-TAX-RATE VALUE 0.0350`)
- Surcharge: flat $25.00 per policy
- Final premium = modPremium + taxAmount + surcharge
- Only processes policies with `POLICY_STATUS = 'AC'`

## Data Types
All monetary fields use `BigDecimal` (never floating-point) per COBOL COMP-3 conventions.

## Testing
```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
cd java-facade && mvn test
```
