# Service Layer — COBOL Migration Log

## POLEND → EndorsementService (2026-05-29)

| Attribute         | Legacy (COBOL)                  | Modern (Java)                              |
|-------------------|---------------------------------|--------------------------------------------|
| Program           | POLEND.cbl                      | EndorsementService.java                    |
| Transaction       | PEND (CICS)                     | POST /api/v1/policies/{num}/endorsements   |
| Data access       | EXEC SQL (DB2 z/OS)             | JdbcTemplate (H2 local / DB2 prod)         |
| Pro-rata calc     | WS-PRORATA-FACTOR (COMP-3)      | BigDecimal with RoundingMode.HALF_UP       |
| Audit trail       | EXEC CICS WRITEQ                | SLF4J logger.info                          |
| Error handling    | WS-ERROR-MSG → 8000-SEND-ERROR  | IllegalArgumentException → HTTP 400        |

### COBOL Paragraph → Java Method Mapping

| COBOL Paragraph            | Java Method / Location                    |
|----------------------------|-------------------------------------------|
| 1000-INITIALIZE            | processEndorsement() entry                |
| 2000-RECEIVE-ENDORSEMENT   | processEndorsement() — findByPolicyNumber |
| 3000-VALIDATE-ENDORSEMENT  | validateEndorsement()                     |
| 4000-CALCULATE-PRORATA     | calculateProrata()                        |
| 5000-APPLY-ENDORSEMENT     | applyEndorsement()                        |
| 6000-WRITE-AUDIT-TRAIL     | writeAuditTrail()                         |
| 7000-SEND-CONFIRMATION     | processEndorsement() — build response     |

### Endorsement Types (88-level values preserved)

| Code | Meaning            |
|------|--------------------|
| CAD  | Coverage Addition   |
| CRM  | Coverage Removal    |
| LCH  | Limit Change        |
| ACH  | Address Change      |
| CAN  | Cancellation        |
