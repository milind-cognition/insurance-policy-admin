# Reference solution: closing the parity gap

The parity gate fails on `main`. That is deliberate — a modernisation that
compiles and passes its unit tests can still be quietly wrong, and the demo is
worth more if the audience sees the wrong answers before they see the fix.

This document records what the differences were, what the fix is, and what the
gate says afterwards. The fix itself lives on the throwaway branch
`devin/reference-fix-parity` (commit `49f32a5`) and is **not merged**.

## Before

`make parity` on `main`:

```
1384/1601 cases identical

160 cases: premium rounding
  PAS-00000001 renewal
  mainframe: ..."branchCode":"NYC2","totalPremium":526.43,"deductible":750.00,"coverageLimit":150000.00,"inceptionDa
  mirror:    ..."branchCode":"NYC2","totalPremium":526.44,"deductible":750.00,"coverageLimit":150000.00,"inceptionDa
  PAS-00000002 renewal
  mainframe: ..."branchCode":"DAL3","totalPremium":527.87,"deductible":1000.00,"coverageLimit":200000.00,"inceptionD
  mirror:    ..."branchCode":"DAL3","totalPremium":527.88,"deductible":1000.00,"coverageLimit":200000.00,"inceptionD

17 cases: renewal date arithmetic
  PAS-00000012 renewal
  mainframe: ...te":"2024-02-29","expiryDate":"2025-02-29","policyholderId":"C000000012","agentCode":"AG1012","branc
  mirror:    ...te":"2024-02-29","expiryDate":"2025-02-28","policyholderId":"C000000012","agentCode":"AG1012","branc
  PAS-00000057 renewal
  mainframe: ...te":"2024-02-29","expiryDate":"2025-02-29","policyholderId":"C000000057","agentCode":"AG1017","branc
  mirror:    ...te":"2024-02-29","expiryDate":"2025-02-28","policyholderId":"C000000057","agentCode":"AG1017","branc

40 cases: renewal eligibility
  PAS-00000009 renewal
  mainframe: ...409 POLICY NOT ELIGIBLE FOR RENEWAL
  mirror:    ...{"policyNumber":"PAS-00000009","policyType":"HLT","policyStatus":"AC","effectiveDate":"2026-10-01","
  PAS-00000019 renewal
  mainframe: ...409 POLICY NOT ELIGIBLE FOR RENEWAL
  mirror:    ...{"policyNumber":"PAS-00000019","policyType":"HLT","policyStatus":"AC","effectiveDate":"2026-08-01","
```

Every unit test in every module is green while this is true. One cent on 160
policies, one day on 17, and 40 policies renewed that the incumbent refuses.

## The three differences

### 1. Premium rounding — a cent, on every renewal

`POLRNW` computes `COMPUTE WS-NEW-PREMIUM = WS-OLD-PREMIUM * 1.05` into a field
declared `PIC S9(09)V99 COMP-3`, with no `ROUNDED` phrase. COBOL truncates the
excess digits. `502.74 * 1.05 = 527.877` is stored as `527.87`.

The mirror used `RoundingMode.HALF_UP`, which is what most people write without
thinking, and got `527.88`.

Fix: `RoundingMode.DOWN` at every point where a monetary value is scaled, which
is what `Money.truncate` in `dropin/contract` already does.

### 2. Renewal eligibility — 40 policies renewed that should not be

`POLRNW` renews policies whose status is `AC` or `EX`. The mirror also accepted
`LP` (lapsed). Nothing in the contract or the copybook says `LP` is renewable;
it was a plausible-looking generalisation, which is exactly how this class of
defect reaches production.

Fix: the renewable set is `AC` and `EX`, as in the COBOL.

### 3. Leap-year renewal date — waived, not fixed

`POLRNW` renews a term with `ADD 10000 TO POLICY-EXPIRY-DATE`, arithmetic on a
`PIC 9(08)` in `YYYYMMDD` form. A policy expiring `2024-02-29` gets an expiry
date of `20250229`, a day that does not exist. The repository README documents
this. The mirror uses calendar arithmetic and produces `2025-02-28`.

**We chose to diverge, and the mirror is right.** Reproducing the bug would mean
storing a non-date in a `DATE` column, and the point of the demo is not that a
mirror can be bug-compatible — it is that a mirror makes the bug *visible and
decidable*. Today the bug is invisible: it is a `MOVE` in a 40-year-old program
that nobody diffs against anything.

So the difference is not silenced, it is **waived**: signed off by name, counted
separately in the report, and it fails the gate again the moment the waiver is
removed.

```
-Dpas.parity.waived="renewal date arithmetic"
```

A waiver is a decision on the record. An unwaived difference is a defect. There
is no third state, and no way to make the gate quiet without writing down why.

## After

On `devin/reference-fix-parity`, with the leap-year divergence waived:

```
1565/1601 cases identical
36 cases waived: renewal date arithmetic (see dropin/docs/REFERENCE-SOLUTION.md)
no unexplained differences: every other case is byte-identical
```

(36 rather than 17 waived cases: with the premium and eligibility defects gone,
leap-term policies that were previously reported under premium rounding — the
report attributes each case to a single root cause — now show their date
difference.)

Reproduce it:

```bash
git checkout devin/reference-fix-parity
cd dropin && make setup
java -Dpas.parity.waived="renewal date arithmetic" \
  -cp "parity/target/pas-parity-1.0.0.jar:legacy/target/pas-legacy-1.0.0.jar:mirror/target/pas-mirror-1.0.0.jar:contract/target/pas-contract-1.0.0.jar:$(cat target/mirror-classpath.txt)" \
  com.acme.dropin.parity.ParityMain
```

One test moved with the fix: `MirrorPolicyAdministrationTest` asserted the
rounded premium `527.88` and now asserts the truncated `527.87`. That is the
test recording the corrected behaviour, not the test being bent to fit.

## What this says to a caller

Nothing above changed a single line of caller code, in either language. The
contract held while the implementation behind it was wrong, and it held while
the implementation was fixed. That is the claim the demo is making: the risk in
a drop-in replacement is not the interface, it is the arithmetic — and the
arithmetic is measurable before anyone cuts over.
