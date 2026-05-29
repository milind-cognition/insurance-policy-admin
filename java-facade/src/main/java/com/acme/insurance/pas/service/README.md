# Service Layer — COBOL Migration Notes

## UNDWRT (Underwriting Decision) → UnderwritingService.java

| Attribute        | COBOL                        | Java                                            |
|------------------|------------------------------|-------------------------------------------------|
| Program          | `UNDWRT.cbl`                 | `UnderwritingService.java`                      |
| CICS Transaction | `PUWR`                       | `POST /api/v1/policies/{policyNumber}/underwriting` |
| Database         | DB2 embedded SQL             | Spring `JdbcTemplate` via `PolicyRepository`    |
| MQ Notification  | `EXEC CICS WRITEQ TD`       | `LOG.info(...)` (placeholder)                   |
| Screen I/O       | BMS Maps `UWRTMAP/UWRTRES`  | JSON request/response DTOs                      |

### Paragraph Mapping

| COBOL Paragraph            | Java Method / Section                          |
|----------------------------|------------------------------------------------|
| `1000-INITIALIZE`          | Parameter validation, `riskScore = 0`          |
| `2000-READ-POLICY-DATA`    | `findByPolicyNumber()` + `findCustomerById()`  |
| `3000-CALCULATE-RISK-SCORE`| Credit base, tier, claims, loss ratio, limits  |
| `4000-CHECK-ACCUMULATION`  | `getAccumulatedLimit()` — $500M branch cap     |
| `5000-RENDER-DECISION`     | Score thresholds: AP/RS/RM/DC                  |
| `6000-WRITE-DECISION`      | `insertUnderwritingDecision()` + `updatePolicyUnderwriting()` |
| `7000-SEND-MQ-NOTIFICATION`| Logged via SLF4J                               |
| `8000-DISPLAY-RESULT`      | Return `UnderwritingResponse` DTO              |

### Risk Score Thresholds

| Score Range | Code | Decision                         |
|-------------|------|----------------------------------|
| 0–299       | AP   | Auto-Accepted: Low Risk          |
| 300–599     | RS   | Referred to Senior Underwriter   |
| 600–799     | RM   | Referred to UW Manager           |
| 800+        | DC   | Auto-Declined: High Risk         |

### Schema Changes

- Added `ACMEINS.CLAIMS` table (referenced by COBOL but missing from original H2 schema)
- Added claim seed data for integration testing

### Files Added/Modified

- **Added**: `model/Customer.java`, `model/UnderwritingDecision.java`, `model/UnderwritingRequest.java`, `model/UnderwritingResponse.java`
- **Added**: `service/UnderwritingService.java`
- **Modified**: `repository/PolicyRepository.java` — new query methods for claims, accumulation, UW writes
- **Modified**: `controller/PolicyController.java` — POST endpoint
- **Modified**: `schema.sql` — CLAIMS table DDL
- **Modified**: `data.sql` — claim seed data
- **Modified**: `PolicyControllerTests.java` — underwriting integration tests
