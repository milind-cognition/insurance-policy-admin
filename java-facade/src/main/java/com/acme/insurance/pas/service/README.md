# Migrated Programs

## POLQRY — Policy Inquiry

| Attribute        | COBOL                                 | Java                                          |
|------------------|---------------------------------------|-----------------------------------------------|
| Program ID       | `POLQRY`                              | `PolicyInquiryService`                        |
| CICS Transaction | `PQRY`                                | `GET /api/v1/policies/{policyNumber}/inquiry`  |
| Author           | J. Henderson (1998-04-15)             | Migrated 2026                                 |
| I/O              | BMS maps (3270 terminal)              | REST/JSON                                     |
| Database         | DB2 z/OS (embedded SQL)               | Same schema via JdbcTemplate                  |

### What the program does

POLQRY is a **read-only policy inquiry** used by CSRs and underwriters to look up
a policy's complete details on a 3270 terminal. Given a policy number, it retrieves
and displays:

1. The **policy header** — type, status, dates, premium, underwriting info
2. The **policyholder (customer)** — name or company, contact information
3. Up to **20 coverages** — type, limit, premium, status, ordered by sequence number

It does not modify any data.

### Data flow

The COBOL program follows a strict sequential paragraph flow. The Java service
replicates this exact sequence:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        0000-MAIN-LOGIC                              │
│                                                                     │
│  1. 1000-RECEIVE-INPUT                                              │
│     ┌──────────────────────────────────────────────────────────┐    │
│     │ COBOL: EXEC CICS RECEIVE MAP('POLQMAP')                  │    │
│     │ Java:  @PathVariable String policyNumber                 │    │
│     │                                                          │    │
│     │ Validates that policy number is not blank.               │    │
│     │ Error: "POLICY NUMBER IS REQUIRED"                       │    │
│     └──────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                    (if valid input)                                  │
│                              ▼                                      │
│  2. 2000-READ-POLICY                                                │
│     ┌──────────────────────────────────────────────────────────┐    │
│     │ SELECT ... FROM POLICIES WHERE POLICY_NUMBER = ?         │    │
│     │                                                          │    │
│     │ Reads the policy header record.                          │    │
│     │ SQLCODE 100 → "POLICY NOT FOUND"                         │    │
│     │ SQLCODE < 0 → "DB2 ERROR READING POLICY"                 │    │
│     │                                                          │    │
│     │ Java: policyRepository.findByPolicyNumber(policyNumber)  │    │
│     │       Returns null if not found → HTTP 404               │    │
│     └──────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                    (if policy found)                                 │
│                              ▼                                      │
│  3. 3000-READ-CUSTOMER                                              │
│     ┌──────────────────────────────────────────────────────────┐    │
│     │ SELECT CUST_ID, CUST_TYPE, LAST_NAME, FIRST_NAME,       │    │
│     │        COMPANY_NAME, PHONE, EMAIL                        │    │
│     │ FROM POLICY_HOLDERS                                      │    │
│     │ WHERE CUST_ID = :POLICY-HOLDER-ID                        │    │
│     │                                                          │    │
│     │ Joins via POLICYHOLDER_ID from the policy record.        │    │
│     │                                                          │    │
│     │ Java: policyRepository.findCustomerById(                 │    │
│     │           policy.getPolicyholderId())                     │    │
│     │                                                          │    │
│     │ NOTE: The Java version selects ALL columns from          │    │
│     │ POLICY_HOLDERS (22 fields) rather than the subset        │    │
│     │ the COBOL program used (7 fields), providing richer      │    │
│     │ data to REST consumers.                                  │    │
│     └──────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  4. 4000-READ-COVERAGES                                             │
│     ┌──────────────────────────────────────────────────────────┐    │
│     │ DECLARE COV_CURSOR CURSOR FOR                            │    │
│     │   SELECT COVERAGE_TYPE, DESCRIPTION, COVERAGE_LIMIT,    │    │
│     │          PREMIUM, STATUS                                 │    │
│     │   FROM COVERAGES                                         │    │
│     │   WHERE POLICY_NUMBER = :POLICY-NUMBER                   │    │
│     │   ORDER BY SEQUENCE_NUM                                  │    │
│     │                                                          │    │
│     │ Fetches rows one at a time (4100-FETCH-COVERAGE) into    │    │
│     │ WS-COVERAGE-TABLE, stopping at 20 rows or end-of-data.  │    │
│     │                                                          │    │
│     │ Java: policyRepository.findCoveragesByPolicyNumber(...)  │    │
│     │       Result capped at 20 via subList(0, 20)             │    │
│     └──────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  5. 5000-DISPLAY-POLICY                                             │
│     ┌──────────────────────────────────────────────────────────┐    │
│     │ COBOL: EXEC CICS SEND MAP('POLQDET') — renders all      │    │
│     │        collected data onto the 3270 BMS map screen.      │    │
│     │                                                          │    │
│     │ Java: Returns PolicyInquiryResponse as JSON via          │    │
│     │       HTTP 200 — contains policy, customer, coverages.   │    │
│     └──────────────────────────────────────────────────────────┘    │
│                                                                     │
│  Error path:                                                        │
│  9000-SEND-ERROR → COBOL sends error text to terminal               │
│                  → Java returns HTTP 400 or 404                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Tables accessed

| Table                      | Operation | Join Key           | Purpose                   |
|----------------------------|-----------|--------------------|---------------------------|
| `ACMEINS.POLICIES`         | SELECT    | `POLICY_NUMBER`    | Policy header record      |
| `ACMEINS.POLICY_HOLDERS`   | SELECT    | `CUST_ID` (via `POLICYHOLDER_ID`) | Customer/policyholder info |
| `ACMEINS.COVERAGES`        | SELECT    | `POLICY_NUMBER`    | Coverage line items       |

### COBOL-to-Java mapping

| COBOL Paragraph       | Lines     | Java Method / Class                          |
|------------------------|-----------|----------------------------------------------|
| `1000-RECEIVE-INPUT`   | 61–76     | `PolicyInquiryService.inquire()` — validation |
| `2000-READ-POLICY`     | 78–105    | `PolicyRepository.findByPolicyNumber()`       |
| `3000-READ-CUSTOMER`   | 107–119   | `PolicyRepository.findCustomerById()`         |
| `4000-READ-COVERAGES`  | 121–136   | `PolicyRepository.findCoveragesByPolicyNumber()` |
| `4100-FETCH-COVERAGE`  | 138–151   | (handled by JdbcTemplate internally)          |
| `5000-DISPLAY-POLICY`  | 153–160   | `PolicyController.inquirePolicy()` — HTTP response |
| `9000-SEND-ERROR`      | 162–168   | Controller returns `ResponseEntity` with 400/404 |
| `9999-RETURN`          | 170–176   | (Spring MVC handles request lifecycle)        |

### COBOL copybooks used

| Copybook              | Java Model         | DB2 Table              |
|-----------------------|--------------------|------------------------|
| `POLICY-RECORD.cpy`   | `Policy.java`      | `ACMEINS.POLICIES`     |
| `CUSTOMER-RECORD.cpy` | `Customer.java`    | `ACMEINS.POLICY_HOLDERS` |
| `COVERAGE-RECORD.cpy` | `Coverage.java`    | `ACMEINS.COVERAGES`    |

### Request / Response

**Request:**
```
GET /api/v1/policies/POL-00000001/inquiry
```

**Response (HTTP 200):**
```json
{
  "policy": {
    "policyNumber": "POL-00000001",
    "policyType": "HOM",
    "policyStatus": "AC",
    "effectiveDate": "2025-01-01",
    "expiryDate": "2026-01-01",
    "policyholderId": "C000000001",
    "totalPremium": 1250.00,
    "uwStatus": "AP",
    "riskScore": 35
  },
  "customer": {
    "custId": "C000000001",
    "custType": "I",
    "lastName": "Smith",
    "firstName": "John",
    "email": "john.smith@example.com",
    "creditScore": 720,
    "riskTier": "A"
  },
  "coverages": [
    {
      "sequenceNum": 1,
      "coverageType": "DWEL",
      "description": "Dwelling Coverage",
      "coverageLimit": 400000.00,
      "premium": 850.00,
      "status": "AC"
    },
    {
      "sequenceNum": 2,
      "coverageType": "PERS",
      "description": "Personal Property",
      "coverageLimit": 100000.00,
      "premium": 400.00,
      "status": "AC"
    }
  ]
}
```

**Error responses:**

| Condition              | COBOL Message                  | HTTP Status | Body  |
|------------------------|--------------------------------|-------------|-------|
| Blank policy number    | `POLICY NUMBER IS REQUIRED`    | 400         | Empty |
| Policy not in database | `POLICY NOT FOUND`             | 404         | Empty |

### Key differences from COBOL

1. **I/O mechanism:** COBOL uses CICS BMS maps (3270 terminal screens); Java uses REST/JSON.
2. **Customer fields:** COBOL selects 7 fields; Java selects all 22 columns for richer API responses.
3. **Coverage fetch:** COBOL uses a DB2 cursor with row-by-row FETCH; Java uses a single query via JdbcTemplate, then caps the result list at 20.
4. **Error handling:** COBOL sends error text to the terminal via `EXEC CICS SEND TEXT`; Java returns HTTP status codes (400, 404).
5. **Session management:** COBOL uses `EXEC CICS RETURN` with COMMAREA for pseudo-conversational flow; Java is stateless REST.

### Files

| File | Purpose |
|------|---------|
| `model/Customer.java` | Customer domain model (maps to `POLICY_HOLDERS`) |
| `model/PolicyInquiryResponse.java` | Combined response DTO |
| `repository/PolicyRepository.java` | Added `findCustomerById()` and `CustomerRowMapper` |
| `service/PolicyInquiryService.java` | POLQRY business logic |
| `controller/PolicyController.java` | Added `/inquiry` endpoint |
