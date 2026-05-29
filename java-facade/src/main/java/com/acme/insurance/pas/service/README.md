# PolicyRenewalService — POLRNW Migration

## 1. Overview

Java translation of the COBOL program `POLRNW` (CICS Transaction `PRWL`).
Processes policy renewals as a Spring Boot `@Service` invoked by a REST PUT endpoint.

## 2. Original COBOL Program

- **Program ID**: POLRNW
- **Transaction**: PRWL
- **Function**: Read an expiring policy, validate renewal eligibility, apply a rate increase with regulatory cap, create a new term, and update active coverages.

## 3. COBOL Paragraph-to-Java Method Mapping

| COBOL Paragraph            | Java Method / Location                        |
|----------------------------|-----------------------------------------------|
| 0000-MAIN-LOGIC            | `PolicyRenewalService.renewPolicy()`          |
| 1000-INITIALIZE            | (inline — initialization at method entry)     |
| 2000-READ-EXISTING-POLICY  | `PolicyRepository.findByPolicyNumber()`       |
| 3000-CHECK-RENEWAL-ELIG.   | Status check (`AC` / `EX`) in `renewPolicy()` |
| 4000-CALCULATE-NEW-PREMIUM | Premium calculation in `renewPolicy()`         |
| 5000-APPLY-RATE-CAP        | Rate cap logic in `renewPolicy()`              |
| 6000-CREATE-RENEWAL-TERM   | `PolicyRepository.updatePolicyRenewal()`      |
| 7000-UPDATE-COVERAGES      | `PolicyRepository.updateCoverageDates()`      |
| 8000-SEND-CONFIRMATION     | `RenewalResponse` DTO returned to controller  |
| 9000-SEND-ERROR            | `IllegalArgumentException` → 400 response     |
| 9999-RETURN                | HTTP response returned to caller               |

## 4. REST Endpoint

```
PUT /api/v1/policies/{policyNumber}/renew
```

- **Success**: `200 OK` with `RenewalResponse` JSON body
- **Policy not found**: `400 BAD REQUEST` — `"POLICY NOT FOUND FOR RENEWAL"`
- **Not eligible**: `400 BAD REQUEST` — `"POLICY NOT ELIGIBLE FOR RENEWAL"`

## 5. Business Rules

| Rule                       | Detail                                      |
|----------------------------|---------------------------------------------|
| Standard rate increase     | 5% (`oldPremium * 1.05`)                    |
| Regulatory rate cap        | 15% maximum increase                        |
| Eligible statuses          | `AC` (Active) or `EX` (Expired)             |
| Renewal count              | Incremented by 1                            |
| New effective date         | Previous expiry date                        |
| New expiry date            | New effective date + 1 year                 |
| Policy status after renew  | `AC`                                        |
| UW status after renew      | `PN` (Pending)                              |
| Updated-by marker          | `POLRNW`                                    |

## 6. Data Flow

1. Controller receives PUT request with `policyNumber`.
2. Service reads the policy from `ACMEINS.POLICIES`.
3. Eligibility check validates status is `AC` or `EX`.
4. New premium calculated with 5% increase, capped at 15%.
5. Policy record updated with new term dates, premium, and renewal count.
6. Active coverages updated with new effective/expiry dates.
7. `RenewalResponse` returned with full renewal details.

## 7. Database Tables Affected

- `ACMEINS.POLICIES` — UPDATE (status, dates, premium, renewal count, UW status)
- `ACMEINS.COVERAGES` — UPDATE (effective/expiry dates for active coverages)

## 8. Key Classes

| Class                  | Package                                  | Role                    |
|------------------------|------------------------------------------|-------------------------|
| `PolicyRenewalService` | `com.acme.insurance.pas.service`         | Business logic          |
| `RenewalResponse`      | `com.acme.insurance.pas.model`           | Response DTO            |
| `PolicyRepository`     | `com.acme.insurance.pas.repository`      | Data access (JDBC)      |
| `PolicyController`     | `com.acme.insurance.pas.controller`      | REST endpoint           |
| `Policy`               | `com.acme.insurance.pas.model`           | Domain model            |

## 9. Testing

Three integration tests in `PolicyControllerTests.java`:

- `renewPolicy_success` — Verifies 200 response, correct premium calculation (890.50 → 935.03), renewal count increment.
- `renewPolicy_notFound` — Verifies 400 for non-existent policy.
- `renewPolicy_cancelledPolicy` — Verifies 400 for policy with status `CN`.

Run tests:
```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
cd java-facade && mvn test
```

## 10. Differences from COBOL Original

| Area                   | COBOL (POLRNW)                 | Java (PolicyRenewalService)    |
|------------------------|--------------------------------|--------------------------------|
| Interface              | BMS screen (CICS RECEIVE MAP)  | REST PUT endpoint              |
| Error handling         | BMS SEND error screen           | HTTP 400 + error message       |
| Transaction control    | CICS SYNCPOINT                 | Spring `@Transactional`        |
| Open claims check      | TODO (never enforced)          | Not implemented (mirrors COBOL)|
| COVID grace period     | Flag exists but = 'N'          | Not implemented (inactive)     |
| Confirmation           | BMS SEND confirmation screen    | JSON response body             |
