# POLQRY — Policy Inquiry Service

## Summary

| Attribute          | Value                                      |
|--------------------|--------------------------------------------|
| COBOL Program      | POLQRY.cbl                                 |
| CICS Transaction   | PQRY                                       |
| Java Service       | PolicyInquiryService                       |
| REST Endpoint      | GET /api/v1/policies/{policyNumber}/inquiry |
| HTTP Methods       | GET (read-only)                            |
| Access Pattern     | Single-policy lookup with related entities |
| Migration Date     | 2026-05-29                                 |

## Description

The POLQRY program is a read-only CICS transaction that retrieves a complete policy inquiry view: policy header, policyholder (customer) details, and associated coverages. It was originally invoked via BMS terminal screens and has been migrated to a REST/JSON endpoint in the `java-facade` Spring Boot application.

The service replicates the COBOL paragraph flow exactly:
1. Validate input (policy number required)
2. Read the policy record
3. Read the customer record via the policy's POLICYHOLDER_ID
4. Read coverages (capped at 20 rows, matching the COBOL OCCURS 20 TIMES limit)
5. Return a combined response

## Data Flow

```
Client Request (GET /api/v1/policies/{policyNumber}/inquiry)
    │
    ▼
PolicyController.inquirePolicy()
    │
    ▼
PolicyInquiryService.inquire(policyNumber)
    │
    ├── 1000: Validate input (blank/null → 400)
    ├── 2000: PolicyRepository.findByPolicyNumber() → ACMEINS.POLICIES
    ├── 3000: PolicyRepository.findCustomerById() → ACMEINS.POLICY_HOLDERS
    ├── 4000: PolicyRepository.findCoveragesByPolicyNumber() → ACMEINS.COVERAGES (cap 20)
    └── 5000: Build PolicyInquiryResponse
    │
    ▼
JSON Response (200 OK / 400 / 404)
```

## Database Tables

| Table                    | Alias    | Access   | Key Column       |
|--------------------------|----------|----------|------------------|
| ACMEINS.POLICIES         | Policy   | SELECT   | POLICY_NUMBER    |
| ACMEINS.POLICY_HOLDERS   | Customer | SELECT   | CUST_ID          |
| ACMEINS.COVERAGES        | Coverage | SELECT   | POLICY_NUMBER    |

## COBOL Paragraph → Java Method Mapping

| COBOL Paragraph      | Java Method / Class                              |
|-----------------------|--------------------------------------------------|
| 0000-MAIN-LOGIC       | PolicyInquiryService.inquire()                   |
| 1000-RECEIVE-INPUT    | Input validation (null/blank check)              |
| 2000-READ-POLICY      | PolicyRepository.findByPolicyNumber()            |
| 3000-READ-CUSTOMER    | PolicyRepository.findCustomerById()              |
| 4000-READ-COVERAGES   | PolicyRepository.findCoveragesByPolicyNumber()   |
| 4100-FETCH-COVERAGE   | (handled by JdbcTemplate RowMapper loop)         |
| 5000-DISPLAY-POLICY   | PolicyInquiryResponse DTO construction           |
| 9000-SEND-ERROR       | ResponseEntity with 400/404 status               |
| 9999-RETURN           | HTTP response returned to client                 |

## Copybook → Model Mapping

| COBOL Copybook Field   | Java Model       | Java Field       | Type            |
|------------------------|------------------|------------------|-----------------|
| WS-POLICY-NUMBER       | Policy           | policyNumber     | String          |
| WS-POLICY-TYPE         | Policy           | policyType       | String          |
| WS-POLICY-STATUS       | Policy           | policyStatus     | String          |
| WS-EFFECTIVE-DATE      | Policy           | effectiveDate    | Date            |
| WS-TOTAL-PREMIUM       | Policy           | totalPremium     | BigDecimal      |
| WS-UW-STATUS           | Policy           | uwStatus         | String          |
| WS-CUST-ID             | Customer         | custId           | String          |
| WS-CUST-LAST-NAME      | Customer         | lastName         | String          |
| WS-CUST-FIRST-NAME     | Customer         | firstName        | String          |
| WS-CUST-CREDIT-SCORE   | Customer         | creditScore      | int             |
| WS-COV-TYPE            | Coverage         | coverageType     | String          |
| WS-COV-LIMIT           | Coverage         | coverageLimit    | BigDecimal      |
| WS-COV-PREMIUM         | Coverage         | premium          | BigDecimal      |

## Request / Response Examples

### Request
```
GET /api/v1/policies/POL-00000001/inquiry HTTP/1.1
Accept: application/json
```

### Response (200 OK)
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
    "city": "Springfield",
    "stateCode": "IL",
    "creditScore": 720
  },
  "coverages": [
    {
      "coverageType": "DWEL",
      "description": "Dwelling Coverage",
      "coverageLimit": 400000.00,
      "premium": 850.00
    },
    {
      "coverageType": "PERS",
      "description": "Personal Property",
      "coverageLimit": 100000.00,
      "premium": 400.00
    }
  ]
}
```

## Error Responses

| HTTP Status | COBOL Equivalent                  | Condition                    |
|-------------|-----------------------------------|------------------------------|
| 400         | "POLICY NUMBER IS REQUIRED"       | Policy number blank or null  |
| 404         | "POLICY NOT FOUND" (SQLCODE 100)  | No matching policy record    |

## Key Differences from COBOL

| Aspect              | COBOL (POLQRY)                        | Java (PolicyInquiryService)          |
|---------------------|---------------------------------------|--------------------------------------|
| Transport           | BMS terminal screen (CICS SEND MAP)   | REST/JSON over HTTP                  |
| Input source        | CICS RECEIVE MAP                      | URL path variable                    |
| Error handling      | BMS error map + CICS RETURN           | HTTP status codes (400, 404)         |
| Coverage cap        | WS-COV-ENTRY OCCURS 20 TIMES         | List.subList(0, min(20, size))       |
| Data types          | PIC/COMP-3 fixed-point               | BigDecimal, Date, String, int        |
| Session management  | CICS pseudo-conversational (TRANSID)  | Stateless HTTP                       |
| DB access           | Embedded SQL with SQLCA               | JdbcTemplate with RowMapper          |

## Files

| File                                       | Description                        |
|--------------------------------------------|------------------------------------|
| `model/Customer.java`                      | Customer domain model (POLICY_HOLDERS) |
| `model/PolicyInquiryResponse.java`         | Combined response DTO              |
| `repository/PolicyRepository.java`         | Added findCustomerById + CustomerRowMapper |
| `service/PolicyInquiryService.java`        | POLQRY business logic              |
| `controller/PolicyController.java`         | Added /inquiry endpoint            |
