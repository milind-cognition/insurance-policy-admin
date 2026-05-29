# Underwriting Service

Migrated from COBOL program **UNDWRT** (CICS transaction `PUWR`).

## Endpoint

```
POST /api/v1/policies/{policyNumber}/underwrite
```

Evaluates underwriting risk for a policy, writes the decision to `UNDERWRITING_DECISIONS`, and updates the policy's `UW_STATUS` and `RISK_SCORE`.

## Risk Scoring Logic

Risk score is cumulative across all factors:

| Factor | Condition | Points |
|---|---|---|
| Credit score | `credit_score > 0`: `(900 - credit_score) / 2` | Varies |
| Credit score | `credit_score <= 0` or null | +400 |
| Risk tier | Substandard (`U`) | +200 |
| Risk tier | Preferred (`P`) | -100 |
| Loss history | Claims count > 3 (last 5 years) | +150 |
| Loss history | Claims count > 5 (cumulative with above) | +200 |
| Loss ratio | `(total_incurred / premium) * 100 > 80` | +200 |
| Coverage limit | > $5,000,000 | +100 |
| Coverage limit | > $10,000,000 (cumulative with above) | +200 |
| Accumulation | Branch total coverage > $500,000,000 | +300 |

## Decision Thresholds

| Score Range | Code | Decision |
|---|---|---|
| 0 - 299 | AP | Auto-Accept |
| 300 - 599 | RS | Refer to Senior Underwriter |
| 600 - 799 | RM | Refer to UW Manager |
| 800+ | DC | Auto-Decline |

## Response

```json
{
  "policyNumber": "POL-00000001",
  "riskScore": 90,
  "decisionCode": "AP",
  "decisionReason": "AUTO-ACCEPTED: LOW RISK",
  "message": "UNDERWRITING EVALUATION COMPLETE"
}
```

## Error Handling

- **400 Bad Request**: Policy/customer data not found.
- Missing CLAIMS table is handled gracefully (treated as 0 claims).

## Original COBOL Paragraph Flow

1. `0000-MAIN-LOGIC` - Orchestration
2. `1000-INITIALIZE` - Clear state, initialize risk score
3. `2000-READ-POLICY-DATA` - Join POLICIES + POLICY_HOLDERS
4. `3000-CALCULATE-RISK-SCORE` - Credit, tier, claims, loss ratio, coverage limit
5. `4000-CHECK-ACCUMULATION` - Branch-level accumulation check
6. `5000-RENDER-DECISION` - Map score to decision code
7. `6000-WRITE-DECISION` - Persist to DB
8. `7000-SEND-MQ-NOTIFICATION` - Skipped in Java migration
9. `8000-DISPLAY-RESULT` / `9000-SEND-ERROR` - Replaced by REST response
