# POLQRY — Policy Inquiry Migration

> **COBOL Program:** `cobol/programs/POLQRY.cbl` → **Java Endpoint:** `GET /api/v1/policies/{policyNumber}/inquiry`

## What This Program Does

POLQRY is a **read-only** CICS transaction (`PQRY`) used by CSRs and underwriters to look up a policy and see its full details in one screen. It combines data from three DB2 tables — `POLICIES`, `POLICY_HOLDERS`, and `COVERAGES` — and displays the result on a BMS map (`POLQDET`).

The Java migration replaces the CICS BMS terminal I/O with a REST/JSON endpoint, preserving the identical query logic, data flow, and security boundaries (e.g., only non-sensitive customer fields are returned).

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Request                                      │
│  COBOL: CICS RECEIVE MAP('POLQMAP') → POLICY-NUMBER                │
│  Java:  GET /api/v1/policies/{policyNumber}/inquiry                 │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  1. Validate Input     │
              │  (1000-RECEIVE-INPUT)  │
              │                        │
              │  Policy number must    │
              │  be non-null/non-empty │
              └───────────┬────────────┘
                          │
                          ▼
              ┌────────────────────────┐
              │  2. Read Policy        │
              │  (2000-READ-POLICY)    │
              │                        │
              │  SELECT ... FROM       │
              │  ACMEINS.POLICIES      │
              │  WHERE POLICY_NUMBER=? │
              └───────────┬────────────┘
                          │
                 ┌────────┴────────┐
                 │                 │
            NOT FOUND           FOUND
                 │                 │
                 ▼                 ▼
           Return 404    ┌─────────────────────┐
                         │ 3. Read Customer    │
                         │ (3000-READ-CUSTOMER)│
                         │                     │
                         │ SELECT 7 cols FROM  │
                         │ ACMEINS.POLICY_     │
                         │ HOLDERS             │
                         │ WHERE CUST_ID =     │
                         │   policy.holder_id  │
                         └────────┬────────────┘
                                  │
                                  ▼
                         ┌─────────────────────┐
                         │ 4. Read Coverages   │
                         │ (4000-READ-         │
                         │  COVERAGES)         │
                         │                     │
                         │ SELECT ... FROM     │
                         │ ACMEINS.COVERAGES   │
                         │ WHERE POLICY_NUMBER │
                         │ ORDER BY SEQ_NUM    │
                         │ (max 20 rows)       │
                         └────────┬────────────┘
                                  │
                                  ▼
                         ┌─────────────────────┐
                         │ 5. Build Response   │
                         │ (5000-DISPLAY-      │
                         │  POLICY)            │
                         │                     │
                         │ COBOL: CICS SEND    │
                         │   MAP to terminal   │
                         │ Java: Return JSON   │
                         │   ResponseEntity    │
                         └─────────────────────┘
```

### Step-by-step

| Step | COBOL Paragraph | Java Method | Description |
|------|-----------------|-------------|-------------|
| 1 | `1000-RECEIVE-INPUT` | `PolicyInquiryService.inquire()` (validation block) | Read input from BMS map / path variable. Reject if blank. |
| 2 | `2000-READ-POLICY` | `PolicyRepository.findByPolicyNumber()` | `SELECT` 17 columns from `ACMEINS.POLICIES`. Return 404 if `SQLCODE=100`. |
| 3 | `3000-READ-CUSTOMER` | `PolicyRepository.findCustomerById()` | `SELECT` **7 non-sensitive columns** from `ACMEINS.POLICY_HOLDERS` using `POLICYHOLDER_ID` from the policy row. |
| 4 | `4000-READ-COVERAGES` / `4100-FETCH-COVERAGE` | `PolicyRepository.findCoveragesByPolicyNumber()` + `subList(0, 20)` | Cursor-based fetch in COBOL; query + list cap in Java. Ordered by `SEQUENCE_NUM`, max 20 rows. |
| 5 | `5000-DISPLAY-POLICY` | Build `PolicyInquiryResponse` DTO | COBOL sends BMS map to terminal; Java returns JSON. |
| err | `9000-SEND-ERROR` | `PolicyController` returns 400/404 `ResponseEntity` | Error display on terminal vs HTTP status codes. |

---

## DB2 Tables Involved

| Table | Schema | Role | Key Column |
|-------|--------|------|------------|
| `POLICIES` | `ACMEINS` | Policy header (status, premium, dates, UW) | `POLICY_NUMBER` |
| `POLICY_HOLDERS` | `ACMEINS` | Customer master | `CUST_ID` (linked via `POLICIES.POLICYHOLDER_ID`) |
| `COVERAGES` | `ACMEINS` | Coverage line items per policy | `POLICY_NUMBER` + `SEQUENCE_NUM` |

---

## COBOL-to-Java File Mapping

| COBOL Source | Java Source | Purpose |
|---|---|---|
| `POLQRY.cbl` (paragraphs 0000–9999) | `PolicyInquiryService.java` | Main orchestration logic |
| `POLQRY.cbl` (2000-READ-POLICY SQL) | `PolicyRepository.FIND_POLICY_SQL` | Policy query (pre-existing) |
| `POLQRY.cbl` (3000-READ-CUSTOMER SQL) | `PolicyRepository.FIND_CUSTOMER_SQL` | Customer query (7 columns) |
| `POLQRY.cbl` (4000/4100 cursor) | `PolicyRepository.FIND_COVERAGES_SQL` | Coverage query (pre-existing) |
| `POLQMAPS` BMS mapset | `PolicyController.inquirePolicy()` | I/O layer (BMS → REST) |
| `CUSTOMER-RECORD.cpy` copybook | `Customer.java` model | Customer data structure |
| `POLICY-RECORD.cpy` copybook | `Policy.java` model (pre-existing) | Policy data structure |
| `COVERAGE-RECORD.cpy` copybook | `Coverage.java` model (pre-existing) | Coverage data structure |
| N/A | `PolicyInquiryResponse.java` | Combined DTO (no COBOL equivalent; BMS map held all fields in WORKING-STORAGE) |

---

## Security: PII Field Restriction

The original COBOL `3000-READ-CUSTOMER` paragraph intentionally selects only **7 non-sensitive columns**:

```sql
SELECT CUST_ID, CUST_TYPE, LAST_NAME, FIRST_NAME,
       COMPANY_NAME, PHONE, EMAIL
FROM POLICY_HOLDERS
WHERE CUST_ID = ?
```

The `POLICY_HOLDERS` table contains additional sensitive fields (`SSN_LAST4`, `TAX_ID`, `CREDIT_SCORE`, `DATE_OF_BIRTH`, etc.) that are **excluded** from this query. The Java migration preserves this restriction — the `CustomerRowMapper` only populates the 7 queried fields. All other `Customer` model fields remain null/default.

This means the inquiry endpoint **never exposes**:
- `ssnLast4` — Social Security Number (last 4)
- `taxId` — Tax Identification Number
- `creditScore` — returns `0` (int default), not the actual score
- `dateOfBirth` — Date of Birth

---

## API Endpoint

```
GET /api/v1/policies/{policyNumber}/inquiry
```

### Responses

| Status | Condition |
|--------|-----------|
| `200 OK` | Policy found — returns full inquiry JSON |
| `400 Bad Request` | Policy number is null, empty, or whitespace |
| `404 Not Found` | No policy with that number exists |

### Example Response (200)

```json
{
  "policy": {
    "policyNumber": "POL-00000001",
    "policyType": "HO",
    "policyStatus": "ACT",
    "effectiveDate": "2024-01-01",
    "expiryDate": "2025-01-01",
    "policyholderId": "C000000001",
    "totalPremium": 1250.00,
    "uwStatus": "APR",
    ...
  },
  "customer": {
    "custId": "C000000001",
    "custType": "I",
    "lastName": "Smith",
    "firstName": "John",
    "companyName": null,
    "phone": "555-0101",
    "email": "john.smith@example.com"
  },
  "coverages": [
    {
      "policyNumber": "POL-00000001",
      "sequenceNum": 1,
      "coverageType": "DWEL",
      "description": "Dwelling Coverage",
      "coverageLimit": 350000.00,
      "premium": 875.00,
      "status": "AC",
      ...
    },
    {
      "policyNumber": "POL-00000001",
      "sequenceNum": 2,
      "coverageType": "LIAB",
      "description": "Personal Liability",
      "coverageLimit": 300000.00,
      "premium": 375.00,
      "status": "AC",
      ...
    }
  ]
}
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| 20-row coverage cap via `subList(0, 20)` | Mirrors COBOL `WS-COV-ENTRY OCCURS 20 TIMES` fixed-size table and the `4000-READ-COVERAGES` loop guard (`WS-COV-COUNT >= 20`) |
| No JPA/Hibernate — uses `JdbcTemplate` | DB2 z/OS driver has compatibility issues with Hibernate dialect; matches existing repository pattern |
| `Customer` model has all 22 fields but only 7 are populated | Model reflects full `POLICY_HOLDERS` DDL for reuse by future programs; the inquiry query restricts what gets populated |
| Error messages match COBOL text | `"POLICY NUMBER IS REQUIRED"` mirrors the COBOL `WS-ERROR-MSG` value for traceability |
| Java 8 bean style (no Lombok, no records) | Consistent with existing `Policy.java` and `Coverage.java`; maintains Java 8 compatibility per `pom.xml` |
