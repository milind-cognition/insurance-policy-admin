# POLNEW — New Policy Creation Service

## Summary

| Attribute         | Value                                      |
|-------------------|--------------------------------------------|
| COBOL Program     | POLNEW.cbl                                 |
| CICS Transaction  | PNEW                                       |
| Java Class        | `NewPolicyService.java`                    |
| REST Endpoint     | `POST /api/v1/policies`                    |
| Operation         | Write (INSERT)                             |
| Status            | Migrated                                   |

## Description

Migrates the POLNEW COBOL program into a Spring Boot REST endpoint. The original program creates new insurance policies on the mainframe via a CICS BMS terminal screen. This Java service replicates the same business logic — input validation, policy number generation, policy insertion, default coverage creation, and (skipped) MQ underwriting referral — exposed as an HTTP POST endpoint returning JSON.

## Data Flow Diagram

```
HTTP POST /api/v1/policies
       │
       ▼
┌──────────────────┐
│ PolicyController  │  ← receives NewPolicyRequest JSON
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ NewPolicyService  │  ← validates, orchestrates inserts
│  (POLNEW logic)   │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ PolicyRepository  │  ← JDBC calls to H2 / DB2
│  (JdbcTemplate)   │
└──────┬───────────┘
       │
       ▼
  ┌─────────┐
  │ DATABASE │  POLICY_HOLDERS, POLICIES, COVERAGES, POLICY_SEQ
  └─────────┘
```

## Tables Accessed

| Table                     | Operation | Purpose                        |
|---------------------------|-----------|--------------------------------|
| ACMEINS.POLICY_HOLDERS    | SELECT    | Validate policyholder exists   |
| ACMEINS.POLICY_SEQ        | NEXT VAL  | Generate policy number         |
| ACMEINS.POLICIES          | INSERT    | Create new policy record       |
| ACMEINS.COVERAGES         | INSERT    | Create default coverage records|

## COBOL-to-Java Mapping

| COBOL Paragraph            | Java Method                              |
|----------------------------|------------------------------------------|
| 0000-MAIN-LOGIC            | `NewPolicyService.createPolicy()`        |
| 1000-INITIALIZE            | (implicit — method entry)                |
| 2000-RECEIVE-MAP           | Controller `@RequestBody` deserialization|
| 3000-VALIDATE-INPUT        | `NewPolicyService.validateInput()`       |
| 4000-GENERATE-POLICY-NUM   | `PolicyRepository.generatePolicyNumber()`|
| 5000-INSERT-POLICY         | `PolicyRepository.insertPolicy()`        |
| 6000-INSERT-COVERAGES      | `NewPolicyService.insertDefaultCoverages()` |
| 6100-WRITE-COVERAGE        | `PolicyRepository.insertCoverage()`      |
| 7000-SEND-MQ-MESSAGE       | Skipped (logged only)                    |
| 8000-SEND-CONFIRMATION     | HTTP 201 + `NewPolicyResponse` JSON      |
| 9000-SEND-ERROR            | HTTP 400 + error JSON                    |
| 9999-RETURN                 | Method return                            |

## Copybook-to-Model Mapping

| COBOL Copybook Field | Java Model Field              | Type         |
|----------------------|-------------------------------|--------------|
| WS-POLICY-NUMBER     | Policy.policyNumber           | String       |
| WS-POLICY-TYPE       | Policy.policyType             | String       |
| WS-POLICY-STATUS     | Policy.policyStatus           | String       |
| WS-EFFECTIVE-DATE    | Policy.effectiveDate          | Date         |
| WS-EXPIRY-DATE       | Policy.expiryDate             | Date         |
| WS-POLICYHOLDER-ID   | Policy.policyholderId         | String       |
| WS-COVERAGE-TYPE     | Coverage.coverageType         | String       |
| WS-COVERAGE-DESC     | Coverage.description          | String       |
| WS-COVERAGE-STATUS   | Coverage.status               | String       |

## Request Example

```json
POST /api/v1/policies
Content-Type: application/json

{
  "policyholderId": "C000000001",
  "policyType": "AUT",
  "effectiveDate": "2027-01-01",
  "expiryDate": "2028-01-01",
  "agentCode": "AG1001",
  "branchCode": "CHI1",
  "totalPremium": 950.00,
  "deductible": 500.00,
  "coverageLimit": 100000.00
}
```

## Response Example

```json
HTTP 201 Created

{
  "policyNumber": "POL-01000000",
  "policyStatus": "PN",
  "message": "Policy created successfully",
  "coveragesCreated": 2
}
```

## Error Responses

| HTTP Status | Error Message                          | COBOL Origin              |
|-------------|----------------------------------------|---------------------------|
| 400         | POLICYHOLDER ID IS REQUIRED            | 3000-VALIDATE-INPUT       |
| 400         | POLICY TYPE IS REQUIRED                | 3000-VALIDATE-INPUT       |
| 400         | EFFECTIVE DATE CANNOT BE IN PAST       | 3000-VALIDATE-INPUT       |
| 400         | POLICY LIMIT MUST BE GREATER THAN ZERO | 3000-VALIDATE-INPUT       |
| 400         | POLICYHOLDER NOT FOUND IN SYSTEM       | 3000-VALIDATE-INPUT       |

## Key Differences from COBOL

| Aspect                | COBOL (POLNEW)              | Java (NewPolicyService)        |
|-----------------------|-----------------------------|--------------------------------|
| Interface             | BMS 3270 terminal screen    | REST/JSON HTTP endpoint        |
| Transaction mgmt      | CICS SYNCPOINT              | Spring @Transactional          |
| MQ referral           | IBM MQ MQPUT                | Skipped (log message only)     |
| Error display         | BMS SEND MAP with ERRMSG   | HTTP 400 + JSON error body     |
| Policy number format  | "POL" + sequence            | "POL-" + zero-padded 8 digits |
| Database              | DB2 z/OS                    | H2 (local) / DB2 (production) |

## Files

| File                          | Description                          |
|-------------------------------|--------------------------------------|
| `model/NewPolicyRequest.java` | Request DTO                          |
| `model/NewPolicyResponse.java`| Response DTO                         |
| `service/NewPolicyService.java`| Business logic (POLNEW paragraphs)  |
| `repository/PolicyRepository.java` | Added insert/sequence methods   |
| `controller/PolicyController.java` | Added POST endpoint             |
