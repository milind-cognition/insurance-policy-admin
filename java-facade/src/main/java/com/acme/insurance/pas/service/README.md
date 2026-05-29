# COBOL-to-Java Migration Documentation

## POLNEW — New Policy Creation

### Summary

| Attribute | COBOL | Java |
|-----------|-------|------|
| Program ID | POLNEW | NewPolicyService |
| CICS Transaction | PNEW | POST /api/v1/policies |
| I/O Mechanism | BMS MAP / COMMAREA | JSON REST (RequestBody/ResponseEntity) |
| Database | DB2 v12 EXEC SQL | Spring JdbcTemplate |
| Message Queue | IBM MQ WRITEQ TD | SLF4J log (non-fatal) |

### Business Function

POLNEW creates new insurance policies in the system. An agent or underwriter submits policyholder ID, policy type, dates, and coverage limit. The program validates the input, verifies the policyholder exists, generates a unique policy number from a DB2 sequence, inserts the policy record with default status (Pending), creates default coverages based on the policy type (Auto, Home, or Commercial), and queues an underwriting referral message.

### Data Flow

```
COBOL POLNEW Paragraph Flow          Java Equivalent
================================     ================================
0000-MAIN-LOGIC                       NewPolicyService.createPolicy()
  |                                     |
  +-- 1000-INITIALIZE                  (implicit - stateless REST)
  |                                     |
  +-- 2000-RECEIVE-MAP                 @RequestBody NewPolicyRequest
  |                                     |
  +-- 3000-VALIDATE-INPUT              validateInput()
  |     |-- Check policyholder ID         throw IAE "POLICYHOLDER ID IS REQUIRED"
  |     |-- Check policy type             throw IAE "POLICY TYPE IS REQUIRED"
  |     |-- Check coverage limit          throw IAE "POLICY LIMIT MUST BE > ZERO"
  |     +-- Verify customer exists        throw IAE "POLICYHOLDER NOT FOUND"
  |                                     |
  +-- 4000-GENERATE-POLICY-NUM         generatePolicyNumber()
  |     +-- NEXT VALUE FOR POLICY_SEQ     jdbcTemplate.queryForObject()
  |                                     |
  +-- 5000-INSERT-POLICY               insertPolicy()
  |     +-- INSERT INTO POLICIES          jdbcTemplate.update()
  |                                     |
  +-- 6000-INSERT-COVERAGES            insertDefaultCoverages()
  |     +-- 6100-WRITE-COVERAGE (loop)    insertCoverage() per type
  |                                     |
  +-- 7000-SEND-MQ-MESSAGE             logger.info() (non-fatal)
  |                                     |
  +-- 8000-SEND-CONFIRMATION           HTTP 201 + Policy JSON
  |                                     |
  +-- 9999-RETURN                      (implicit)
```

### Tables Accessed

| Table | Operation | Join Key | Purpose |
|-------|-----------|----------|---------|
| ACMEINS.POLICY_HOLDERS | SELECT | CUST_ID | Validate policyholder exists |
| ACMEINS.POLICIES | INSERT | POLICY_NUMBER | Create new policy record |
| ACMEINS.COVERAGES | INSERT | POLICY_NUMBER + SEQ | Create default coverages |
| ACMEINS.POLICY_SEQ | NEXT VALUE | — | Generate policy number |

### COBOL-to-Java Mapping

| COBOL Paragraph | Lines | Java Method | Class |
|----------------|-------|-------------|-------|
| 0000-MAIN-LOGIC | 96-115 | createPolicy() | NewPolicyService |
| 1000-INITIALIZE | 117-120 | (implicit) | — |
| 2000-RECEIVE-MAP | 122-130 | @RequestBody | PolicyController |
| 3000-VALIDATE-INPUT | 132-166 | validateInput() | NewPolicyService |
| 4000-GENERATE-POLICY-NUM | 168-176 | generatePolicyNumber() | NewPolicyService |
| 5000-INSERT-POLICY | 178-210 | insertPolicy() | NewPolicyService |
| 6000-INSERT-COVERAGES | 212-236 | insertDefaultCoverages() | NewPolicyService |
| 6100-WRITE-COVERAGE | 238-253 | insertCoverage() | NewPolicyService |
| 7000-SEND-MQ-MESSAGE | 255-261 | logger.info() | NewPolicyService |
| 8000-SEND-CONFIRMATION | 263-265 | HTTP 201 response | PolicyController |
| 9000-SEND-ERROR | 267-273 | HTTP 400 response | PolicyController |
| 9999-RETURN | 275-281 | (implicit) | — |

### Copybook-to-Model Mapping

| Copybook | Java Model |
|----------|-----------|
| POLICY-RECORD.cpy | Policy.java |
| COVERAGE-RECORD.cpy | Coverage.java |
| CUSTOMER-RECORD.cpy | Customer.java |

### Request/Response Examples

**Request:** `POST /api/v1/policies`
```json
{
  "policyholderId": "C000000001",
  "policyType": "HOM",
  "effectiveDate": "2026-07-01",
  "expiryDate": "2027-07-01",
  "agentCode": "AG1001",
  "branchCode": "CHI1",
  "totalPremium": 1500.00,
  "deductible": 1000.00,
  "coverageLimit": 600000.00
}
```

**Response (201 Created):**
```json
{
  "policyNumber": "POL001000000",
  "policyType": "HOM",
  "policyStatus": "PN",
  "effectiveDate": "2026-07-01",
  "expiryDate": "2027-07-01",
  "policyholderId": "C000000001",
  "agentCode": "AG1001",
  "branchCode": "CHI1",
  "totalPremium": 1500.00,
  "deductible": 1000.00,
  "coverageLimit": 600000.00,
  "renewalCount": 0,
  "uwStatus": "PN",
  "riskScore": 0,
  "webIndicator": "N",
  "apiFlag": "N",
  "updatedBy": "POLNEW"
}
```

### Error Responses

| Condition | COBOL Error Message | HTTP Status | Response Body |
|-----------|-------------------|-------------|---------------|
| Missing policyholder ID | POLICYHOLDER ID IS REQUIRED | 400 | `{"error": "POLICYHOLDER ID IS REQUIRED"}` |
| Missing policy type | POLICY TYPE IS REQUIRED | 400 | `{"error": "POLICY TYPE IS REQUIRED"}` |
| Zero/null coverage limit | POLICY LIMIT MUST BE GREATER THAN ZERO | 400 | `{"error": "POLICY LIMIT MUST BE GREATER THAN ZERO"}` |
| Policyholder not in DB | POLICYHOLDER NOT FOUND IN SYSTEM | 400 | `{"error": "POLICYHOLDER NOT FOUND IN SYSTEM"}` |

### Key Differences from COBOL

1. **I/O**: BMS MAP replaced by JSON REST request/response
2. **MQ**: IBM MQ WRITEQ TD replaced by SLF4J log (non-fatal, easily replaced with JMS later)
3. **Session**: CICS pseudo-conversational COMMAREA replaced by stateless REST
4. **Policy number**: Same POLICY_SEQ sequence, same formatting (POL + 9-digit padded)
5. **Error handling**: COBOL SEND TEXT replaced by HTTP 400 with JSON error body
6. **Default coverages**: Same type-to-coverage mapping preserved exactly

### Files

| File | Purpose |
|------|---------|
| `model/NewPolicyRequest.java` | Request DTO for policy creation input |
| `model/Customer.java` | Customer domain model (POLICY_HOLDERS table) |
| `service/NewPolicyService.java` | Service mirroring POLNEW paragraph flow |
| `repository/PolicyRepository.java` | Added insert methods, customer lookup, sequence |
| `controller/PolicyController.java` | Added POST /api/v1/policies endpoint |
| `PolicyControllerTests.java` | 4 new integration tests for POLNEW |
