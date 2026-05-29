# Service Layer — COBOL-to-Java Migration Log

## RenewalService (POLRNW → Java)

| Attribute        | Value                                            |
|------------------|--------------------------------------------------|
| COBOL Program    | `POLRNW` (`cobol/programs/POLRNW.cbl`)           |
| CICS Transaction | `PRWL`                                           |
| Java Class       | `RenewalService.java`                            |
| REST Endpoint    | `POST /api/v1/policies/{policyNumber}/renewal`   |
| Migrated By      | Devin (automated migration)                      |
| Date             | 2026-05-29                                       |

### COBOL Paragraph → Java Method Mapping

| COBOL Paragraph                 | Java Method / Logic                              |
|---------------------------------|--------------------------------------------------|
| 1000-INITIALIZE                 | Method entry (no CICS date needed)               |
| 2000-READ-EXISTING-POLICY       | `PolicyRepository.findByPolicyNumber()`          |
| 3000-CHECK-RENEWAL-ELIGIBILITY  | `checkRenewalEligibility()` — AC or EX only      |
| 4000-CALCULATE-NEW-PREMIUM      | `calculateNewPremium()` — 5% flat increase       |
| 5000-APPLY-RATE-CAP             | `applyRateCap()` — 15% regulatory cap            |
| 6000-CREATE-RENEWAL-TERM        | Advance dates +1 year, `updatePolicyForRenewal()`|
| 7000-UPDATE-COVERAGES           | `PolicyRepository.updateCoverageDates()`         |
| 8000-SEND-CONFIRMATION          | Return `RenewalResponse` DTO                     |

### Business Rules Preserved

- **Rate increase**: Flat 5% (`oldPremium * 1.05`).
- **Rate cap**: 15% maximum increase. If exceeded, premium is recalculated at cap.
- **Eligible statuses**: `AC` (active) or `EX` (expired).
- **New term dates**: Effective = old expiry; new expiry = effective + 1 year.
- **UW status**: Set to `PN` (pending) on renewal.
- **UPDATED_BY**: Set to `POLRNW` to match COBOL program ID.

### Items Not Migrated

- **Open claims check** (paragraph 3000): Commented out in COBOL as "never fully implemented".
- **COVID grace period flag** (`WS-COVID-GRACE`): Declared but not used in current logic.
- **BMS map I/O** (paragraphs 8000/9000): Replaced by REST request/response.
