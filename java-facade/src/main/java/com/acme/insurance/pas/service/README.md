# POLQRY Migration: COBOL to Java REST

## Summary

| Attribute | COBOL | Java |
|---|---|---|
| Program/Class | `POLQRY.cbl` | `PolicyInquiryService.java` |
| Transaction | `PQRY` (CICS) | `GET /api/v1/policies/{policyNumber}/inquiry` |
| Type | Read-only inquiry | Read-only GET endpoint |
| Interface | BMS Maps (3270 terminal) | REST/JSON over HTTP |
| Database | DB2 v12 embedded SQL | JdbcTemplate with RowMapper |
| Error handling | BMS SEND TEXT | HTTP status codes (400, 404) |

## Data Flow Diagram

```
COBOL POLQRY Flow:
  1000-RECEIVE-INPUT  -->  Validate policy number from BMS map
  2000-READ-POLICY    -->  SELECT from POLICIES
  3000-READ-CUSTOMER  -->  SELECT from POLICY_HOLDERS
  4000-READ-COVERAGES -->  CURSOR over COVERAGES (max 20)
  5000-DISPLAY-POLICY -->  SEND combined view to BMS map

Java REST Flow:
  PolicyController.inquirePolicy()
    --> PolicyInquiryService.inquirePolicy(policyNumber)
        1. Validate input (IllegalArgumentException -> 400)
        2. PolicyRepository.findByPolicyNumber() -> null means 404
        3. PolicyRepository.findCustomerById()
        4. PolicyRepository.findCoveragesByPolicyNumber() -> cap at 20
        5. Assemble PolicyInquiryResponse -> 200
```

## Tables Accessed

| Table | Schema | Operation | COBOL Paragraph | Java Method |
|---|---|---|---|---|
| `POLICIES` | `ACMEINS` | SELECT by PK | `2000-READ-POLICY` | `PolicyRepository.findByPolicyNumber()` |
| `POLICY_HOLDERS` | `ACMEINS` | SELECT by PK | `3000-READ-CUSTOMER` | `PolicyRepository.findCustomerById()` |
| `COVERAGES` | `ACMEINS` | SELECT + ORDER BY | `4000-READ-COVERAGES` | `PolicyRepository.findCoveragesByPolicyNumber()` |

## COBOL-to-Java Mapping

| COBOL Paragraph | Java Method | Description |
|---|---|---|
| `0000-MAIN-LOGIC` | `PolicyController.inquirePolicy()` | Orchestrates the flow |
| `1000-RECEIVE-INPUT` | Input validation in `PolicyInquiryService.inquirePolicy()` | Validates policy number is not blank |
| `2000-READ-POLICY` | `PolicyRepository.findByPolicyNumber()` | Reads policy header record |
| `3000-READ-CUSTOMER` | `PolicyRepository.findCustomerById()` | Reads customer/policyholder record |
| `4000-READ-COVERAGES` | `PolicyRepository.findCoveragesByPolicyNumber()` + `subList(0, 20)` | Reads coverages with max 20 cap |
| `4100-FETCH-COVERAGE` | (handled by JdbcTemplate RowMapper) | Individual row fetch from cursor |
| `5000-DISPLAY-POLICY` | `PolicyInquiryResponse` DTO assembly | Combines policy + customer + coverages |
| `9000-SEND-ERROR` | HTTP 400/404 `ResponseEntity` | Error response |

## Copybook Mapping

| COBOL Copybook | Java Model | DB2 Table |
|---|---|---|
| `POLICY-RECORD` | `Policy.java` | `ACMEINS.POLICIES` |
| `CUSTOMER-RECORD` | `Customer.java` | `ACMEINS.POLICY_HOLDERS` |
| `COVERAGE-RECORD` | `Coverage.java` | `ACMEINS.COVERAGES` |
| `WS-COVERAGE-TABLE` (OCCURS 20) | `List<Coverage>` (capped at 20) | `ACMEINS.COVERAGES` |

## Request/Response Examples

### Request

```
GET /api/v1/policies/POL-00000001/inquiry
```

### Success Response (200 OK)

```json
{
  "policy": {
    "policyNumber": "POL-00000001",
    "policyType": "HOM",
    "policyStatus": "AC",
    "effectiveDate": "2025-01-01",
    "expiryDate": "2026-01-01",
    "policyholderId": "C000000001",
    "agentCode": "AG1001",
    "branchCode": "CHI1",
    "totalPremium": 1250.00,
    "deductible": 1000.00,
    "coverageLimit": 500000.00,
    "inceptionDate": "2020-01-01",
    "renewalCount": 5,
    "uwStatus": "AP",
    "riskScore": 35,
    "webIndicator": "Y",
    "apiFlag": "Y"
  },
  "customer": {
    "custId": "C000000001",
    "custType": "I",
    "lastName": "Smith",
    "firstName": "John",
    "middleInit": "A",
    "addrLine1": "123 Main St",
    "city": "Springfield",
    "stateCode": "IL",
    "zipCode": "62701",
    "phone": "217-555-0100",
    "email": "john.smith@example.com",
    "dateOfBirth": "1975-06-15",
    "creditScore": 720,
    "riskTier": "A"
  },
  "coverages": [
    {
      "policyNumber": "POL-00000001",
      "sequenceNum": 1,
      "coverageType": "DWEL",
      "description": "Dwelling Coverage",
      "coverageLimit": 400000.00,
      "deductible": 1000.00,
      "premium": 850.00,
      "status": "AC"
    },
    {
      "policyNumber": "POL-00000001",
      "sequenceNum": 2,
      "coverageType": "PERS",
      "description": "Personal Property",
      "coverageLimit": 100000.00,
      "deductible": 500.00,
      "premium": 400.00,
      "status": "AC"
    }
  ],
  "coverageCount": 2
}
```

## Error Responses

| Scenario | COBOL Error Message | HTTP Status | Description |
|---|---|---|---|
| Blank/missing policy number | `POLICY NUMBER IS REQUIRED` | `400 Bad Request` | Input validation failure |
| Policy not found | `POLICY NOT FOUND` | `404 Not Found` | No matching POLICIES record |
| DB2 read error | `DB2 ERROR READING POLICY` | `500 Internal Server Error` | Database-level exception (Spring default) |

## Key Differences from COBOL

| Aspect | COBOL POLQRY | Java REST |
|---|---|---|
| Interface | BMS 3270 terminal map | HTTP/JSON REST API |
| Client | CICS terminal user (CSR) | Any HTTP client |
| Session | Pseudo-conversational CICS | Stateless HTTP |
| Error display | `CICS SEND TEXT` to terminal | HTTP status codes |
| Coverage cursor | DB2 DECLARE/OPEN/FETCH/CLOSE | JdbcTemplate `query()` returns `List` |
| Coverage cap | `WS-COV-ENTRY OCCURS 20 TIMES` | `subList(0, Math.min(size, 20))` |
| Premium details | Read via `PREMIUM-RECORD` copybook | Not yet exposed (future enhancement) |
| Transaction ID | `PQRY` | N/A (REST endpoint path) |
| Authentication | RACF/CICS security | None (VPN segmentation; OAuth2 planned) |
