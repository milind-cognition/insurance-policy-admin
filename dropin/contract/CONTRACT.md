# What a caller of the Policy Administration System is entitled to depend on

Audience: an engineer who owns a .NET or Java application that calls this system
and has never opened a copybook.

There are two layers. The **service boundary** is the contract your application
is written against: HTTP, a URL, a JSON or XML document. The **COMMAREA** is the
fixed-width record the CICS program is handed underneath that boundary. Your
application depends on the first one. The second one explains why the first one
looks the way it does, and it is where the interesting failure modes live.

Everything below is produced from the code in this repository, not transcribed
from it. The layout tables come from `Copybook`, which parses
`cobol/copybooks/*.cpy`; the OpenAPI document comes from `OpenApiGenerator`,
which reads `java-facade`'s controller, models and repository; the JSON bodies
were captured from the running incumbent by
`dropin/scripts/capture-golden-fixtures.sh`.

---

## Layer 1 - the service boundary (this is the contract)

Two read operations, both `GET`, both unauthenticated inside the network
perimeter today:

| Operation | Path | 200 | 404 |
|---|---|---|---|
| `getPolicy` | `/api/v1/policies/{policyNumber}` | policy document | empty body |
| `getCoverages` | `/api/v1/policies/{policyNumber}/coverages` | array of coverage documents, ordered by sequence number | empty body |

The machine-readable version is
[`src/main/resources/openapi/pas-v1.yaml`](src/main/resources/openapi/pas-v1.yaml),
regenerated and diffed by `ContractDriftTest`, so it cannot fall behind the
controller it was generated from.

A policy, exactly as the incumbent returns it today
(`src/main/resources/golden/POL-00000001.policy.json`):

```json
{"policyNumber":"POL-00000001","policyType":"HOM","policyStatus":"AC","effectiveDate":"2025-01-01","expiryDate":"2026-01-01","policyholderId":"C000000001","agentCode":"AG1001","branchCode":"CHI1","totalPremium":1250.00,"deductible":1000.00,"coverageLimit":500000.00,"inceptionDate":"2020-01-01","renewalCount":5,"uwStatus":"AP","riskScore":35,"webIndicator":"Y","apiFlag":"Y","lastUpdated":0,"updatedBy":"TNGUYEN"}
```

The same policy in the XML projection, for callers going through CICS Web
Services rather than the REST facade - same fields, same order, same values:

```xml
<policy><policyNumber>POL-00000001</policyNumber><policyType>HOM</policyType><policyStatus>AC</policyStatus><effectiveDate>2025-01-01</effectiveDate><expiryDate>2026-01-01</expiryDate><policyholderId>C000000001</policyholderId><agentCode>AG1001</agentCode><branchCode>CHI1</branchCode><totalPremium>1250.00</totalPremium><deductible>1000.00</deductible><coverageLimit>500000.00</coverageLimit><inceptionDate>2020-01-01</inceptionDate><renewalCount>5</renewalCount><uwStatus>AP</uwStatus><riskScore>35</riskScore><webIndicator>Y</webIndicator><apiFlag>Y</apiFlag><lastUpdated>0</lastUpdated><updatedBy>TNGUYEN</updatedBy></policy>
```

### You may depend on

1. **Field names and their order.** `PolicyView` and `CoverageView` are records
   whose components are checked against the incumbent's model classes field by
   field (`ContractDriftTest.contractRecordsCarryExactlyTheFacadeFieldsInOrder`).
2. **Two decimal places on every money field, always rendered.** `1250.00`, not
   `1250.0` and not `"1250.00"`. DB2 stores `DECIMAL(n,2)`, the copybook stores
   `COMP-3` with `V99`, and the JSON keeps the trailing zero. A strict client
   that compares response bodies, or a deserializer targeting a fixed-scale
   decimal, will notice if this changes.
3. **Dates as `YYYY-MM-DD` strings.** `effectiveDate`, `expiryDate`,
   `inceptionDate` and the coverage dates.
4. **`lastUpdated` as epoch milliseconds, not a date string.** It is the one
   field the facade reads with `rs.getTimestamp` instead of `rs.getDate`, and
   Jackson serializes it as a number as a result. This is an accident of the
   incumbent's implementation that is nonetheless part of what callers parse
   today, which is exactly the kind of detail a rewrite loses.
5. **Strings arrive trimmed.** The database and the COMMAREA are fixed width and
   space padded; the facade calls `.trim()` on the way out. `"HOM"`, not
   `"HOM "`.
6. **404 with an empty body for an unknown policy number**, including on the
   coverages path, which checks the policy exists first. This is the service
   boundary's rendering of DB2 `SQLCODE 100` and of the `POLICY NOT FOUND`
   branch in `POLQRY.cbl`.
7. **Coverages ordered by sequence number**, because the query says
   `ORDER BY SEQUENCE_NUM` and the COBOL cursor does the same.

### You may not depend on

- `null` versus absent for `agentCode`, `branchCode`, `description`,
  `ratingTerritory` and `classCode`: the repository maps these defensively, so
  they can be `null` where the others cannot.
- Any field not in the list above appearing later. New fields may be added.
- HTTP response headers, whitespace, or the `X-Application-Context` header the
  incumbent happens to emit.
- Time of day in `lastUpdated` surviving a round trip through the COMMAREA - see
  below.

---

## Layer 2 - the COMMAREA underneath it

A CICS program does not receive JSON. It receives a byte-addressed record whose
shape is fixed by the copybook. `POLQRY`, `POLRNW` and `POLNEW` all declare a
`PIC X(256)` COMMAREA and lay the records below into it.

`POLICY-RECORD` (`cobol/copybooks/POLICY-RECORD.cpy`), 125 bytes:

```
FIELD                        OFFSET LEN    PICTURE
POLICY-NUMBER                0      12     PIC X(12)
POLICY-TYPE                  12     3      PIC X(03)
POLICY-STATUS                15     2      PIC X(02)
POLICY-EFFECTIVE-DATE        17     8      PIC 9(08)
POLICY-EXPIRY-DATE           25     8      PIC 9(08)
POLICY-HOLDER-ID             33     10     PIC X(10)
POLICY-AGENT-CODE            43     6      PIC X(06)
POLICY-BRANCH-CODE           49     4      PIC X(04)
POLICY-TOTAL-PREMIUM         53     6      PIC S9(09)V99 COMP-3
POLICY-DEDUCTIBLE            59     5      PIC S9(07)V99 COMP-3
POLICY-LIMIT                 64     7      PIC S9(11)V99 COMP-3
POLICY-INCEPTION-DATE        71     8      PIC 9(08)
POLICY-RENEWAL-COUNT         79     3      PIC 9(03)
POLICY-UW-STATUS             82     2      PIC X(02)
POLICY-RISK-SCORE            84     3      PIC 9(03)
POLICY-WEB-IND               87     1      PIC X(01)
POLICY-API-FLAG              88     1      PIC X(01)
POLICY-LAST-UPDATED          89     8      PIC 9(08)
POLICY-UPDATED-BY            97     8      PIC X(08)
FILLER                       105    20     PIC X(20)
```

`COVERAGE-RECORD` (`cobol/copybooks/COVERAGE-RECORD.cpy`), 124 bytes:

```
FIELD                        OFFSET LEN    PICTURE
COV-POLICY-NUMBER            0      12     PIC X(12)
COV-SEQUENCE-NUM             12     3      PIC 9(03)
COV-TYPE-CODE                15     4      PIC X(04)
COV-DESCRIPTION              19     40     PIC X(40)
COV-LIMIT                    59     7      PIC S9(11)V99 COMP-3
COV-DEDUCTIBLE               66     5      PIC S9(07)V99 COMP-3
COV-PREMIUM                  71     6      PIC S9(09)V99 COMP-3
COV-EFFECTIVE-DATE           77     8      PIC 9(08)
COV-EXPIRY-DATE              85     8      PIC 9(08)
COV-STATUS                   93     2      PIC X(02)
COV-COINSURANCE-PCT          95     3      PIC 9(03)
COV-RATING-TERRITORY         98     6      PIC X(06)
COV-CLASS-CODE               104    5      PIC X(05)
FILLER                       109    15     PIC X(15)
```

The same policy as the bytes a CICS program would see
(`PolicyCommarea.encode`, printed as hex):

```
504F4C2D3030303030303031 484F4D 4143 3230323530313031 3230323630313031 43303030303030303031 414731303031 43484931 00000125000C 000100000C 0000050000000C ...
```

Three things to read out of that:

- `504F4C2D3030303030303031` is `POL-00000001` in ASCII. On z/OS it would be
  EBCDIC. The encoding is a transport concern; the *layout* is the contract.
- `00000125000C` is `POLICY-TOTAL-PREMIUM`: eleven digits packed two per byte,
  `C` in the final nibble meaning positive, an implied decimal point two digits
  from the right. It reads as `1250.00`.
- Text fields are space padded to their `PIC X(n)` width. `TNGUYEN` occupies
  eight bytes as `"TNGUYEN "`.

### Where the two layers genuinely disagree

`lastUpdated` is a timestamp on the service boundary and `PIC 9(08)`, a date, on
the COMMAREA. A value that goes through the COMMAREA comes back with its time of
day set to midnight UTC. `CommareaRoundTripTest` documents this rather than
hiding it.

---

## The mapping between the two

Field names are not the same on both sides, which is why the mapping is code
(`CommareaMapping`) with a test that fails if a field appears on one side and
not the other. The ones that would catch you out:

| Service boundary | COMMAREA |
|---|---|
| `coverageLimit` (policy) | `POLICY-LIMIT` |
| `coverageLimit` (coverage) | `COV-LIMIT` |
| `policyholderId` | `POLICY-HOLDER-ID` |
| `webIndicator` | `POLICY-WEB-IND` |
| `coverageType` | `COV-TYPE-CODE` |

Every JSON field also carries its COMMAREA field, picture, offset and length as
`x-cobol-*` extensions in `pas-v1.yaml`, so a caller generating a client from
the OpenAPI document can see the layer underneath without reading COBOL.

---

## What is not pinned here, and why

- **`PNEW`/`POLNEW` and `PRWL`/`POLRNW` have no REST projection today.** The
  incumbent facade is read-only; the README says so and the controller has no
  write endpoints. Their COMMAREA layouts are pinned (they use the same
  `POLICY-RECORD` and `COVERAGE-RECORD` copybooks), and `dropin/mirror`
  implements the renewal logic, but no HTTP contract for them exists to pin.
- **Character encoding.** ASCII here, EBCDIC on z/OS. Not exercised.
- **CICS response codes, MQ, and the batch cycle.** Out of scope for the caller
  boundary; see the "what this does not prove" section of `dropin/README.md`.
