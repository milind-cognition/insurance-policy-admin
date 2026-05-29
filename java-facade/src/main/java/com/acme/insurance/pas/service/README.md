# PolicyEndorsementService

Migrated from COBOL program **POLEND** (CICS transaction `PEND`).

## Endpoint

```
POST /api/v1/policies/{policyNumber}/endorsements
```

### Request Body

```json
{
  "endorsementType": "CAD",
  "description": "Add flood coverage",
  "premiumAdjustment": 150.00
}
```

### Response (201 Created)

```json
{
  "policyNumber": "POL-00000001",
  "endorsementSeq": 1,
  "endorsementType": "CAD",
  "premiumAdjustment": 150.00,
  "prorataFactor": 0.583333,
  "newTotalPremium": 1400.00,
  "message": "ENDORSEMENT PROCESSED SUCCESSFULLY"
}
```

## Business Rules (from POLEND paragraphs)

| Rule | Source Paragraph |
|------|-----------------|
| Only active (`AC`) policies can be endorsed | 3000-VALIDATE-ENDORSEMENT |
| Valid types: `CAD`, `CRM`, `LCH`, `ACH`, `CAN` | 3000-VALIDATE-ENDORSEMENT |
| Pro-rata factor = days remaining / days in term | 4000-CALCULATE-PRORATA |
| Premium adjustment added to policy total | 5000-APPLY-ENDORSEMENT |
| Endorsement seq auto-incremented (MAX + 1) | 5000-APPLY-ENDORSEMENT |
| Audit trail logged (replaces CICS TS queue) | 6000-WRITE-AUDIT-TRAIL |

## Endorsement Types

| Code | Description |
|------|-------------|
| CAD  | Coverage Addition |
| CRM  | Coverage Removal |
| LCH  | Limit Change |
| ACH  | Address Change |
| CAN  | Cancellation |

## Error Responses (400 Bad Request)

- `POLICY NOT FOUND`
- `ONLY ACTIVE POLICIES CAN BE ENDORSED`
- `ENDORSEMENT TYPE IS REQUIRED`
- `INVALID ENDORSEMENT TYPE`
