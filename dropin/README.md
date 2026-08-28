# Drop-in replacement, demonstrated

> "Today we have .NET or Java applications calling these mainframe programs —
> would this be a drop-in replacement in terms of this platform?"

The answer this directory exists to *show* rather than assert: **the calling
application does not change. Only what sits behind the call changes.**

Nothing under `cobol/`, `jcl/`, `sql/` or `java-facade/` was modified. The
incumbent Java facade in this repository is run here as-is, from its own jar,
against both implementations.

## Run it

Needs a JDK 21, Maven, and the .NET 8 SDK. No credentials, no mainframe, no
paid services, nothing to configure.

```bash
cd dropin
make setup    # ~2 min: builds four Java modules and the .NET client
make test     # unit tests, both languages - these pass
make parity   # the differential gate - this FAILS, on purpose
```

**Success looks like:** `make test` green in Java and .NET, `make proof`
printing `identical` for every policy, and `make parity` printing
`1384/1601 cases identical` — a green build sitting on top of wrong answers.

## What is here

| | |
|---|---|
| `contract/` | The caller's entitlement, as executable truth. OpenAPI 3 generated from the incumbent controller, golden JSON captured from the running incumbent, the XML projection of the same payloads, and the COMMAREA copybook layouts underneath — parsed from the real copybooks, not retyped. |
| `legacy/` | The incumbent behaviour of `POLQRY` and `POLRNW` transliterated statement by statement, so there is something to compare against without a CICS region. Its arithmetic is cross-checked against GnuCOBOL where `cobc` is installed. |
| `mirror/` | Java 21 + Spring Boot 3 on an embedded database whose schema is translated at runtime from `sql/ddl/create-tables.sql`. |
| `switch/` | One service, one contract, `pas.backend=mainframe\|mirror`. The seam is a single Spring binding. |
| `clients/dotnet/` | A .NET 8 caller and an xUnit test that asserts both backends answer it identically. |
| `parity/` | 1601 cases across 400 deterministic policies, compared byte for byte. |
| `docs/REFERENCE-SOLUTION.md` | What the parity failures are, and the fix — on an unmerged branch. |

## The contract comes first

Progressive's estate is fronted by CICS Web Services exchanging XML/JSON, so
that service boundary is the primary contract here: `contract/CONTRACT.md`
leads with the JSON and XML documents a caller is entitled to, and documents the
COMMAREA layout as the layer underneath it. Both are pinned by tests, and
`ContractDriftTest` regenerates the OpenAPI document from the incumbent source
on every build — if the facade changes and the contract does not, the build
fails.

## The two proofs

**The incumbent Java caller, unmodified.** `make proof` starts
`java-facade/target/pas-facade-1.0.0.jar` against its own H2 database, then
starts the *same jar, same profile, same command line* against the mirror's
database, and diffs every byte of every response:

```
identical  POL-00000001
identical  POL-00000001-coverages
...
The unmodified incumbent facade returned byte-identical JSON from both backends.
```

**The .NET caller.** `make dotnet-test` runs a C# client against the service on
both backends and asserts the JSON, the XML and the 404 are identical. The
client has no setting that says which backend it is talking to, because there
isn't one.

## Green build, wrong answers

`make parity` fails today on three realistic differences: premium rounding
(COBOL truncates a `COMP-3` field with no `ROUNDED` phrase; the mirror rounded
half-up — one cent, 160 policies), renewal eligibility (the mirror renewed
lapsed policies the incumbent refuses), and the leap-year renewal-date bug the
repository README documents. See `docs/REFERENCE-SOLUTION.md` for which of the
three we chose to fix, which we chose to keep diverging, and why.

CI runs unit tests as a required job and parity as a separate, informational
job — so the failing gate is visible on every commit without pretending it is a
broken build.

## What this does NOT prove

- **No real CICS region.** There is no `EXEC CICS LINK`, no COMMAREA on the
  wire, no transaction manager, no pseudo-conversational state, no CICS web
  service pipeline. The COMMAREA layouts are exercised as fixed-width byte
  images in tests, not against a region.
- **No real DB2.** The mirror runs H2 in DB2 mode on a schema translated from
  `sql/ddl/create-tables.sql`; DB2-only storage clauses (tablespaces,
  sequences) are dropped, and `Db2Ddl.droppedStatements()` reports exactly
  which. Isolation levels, locking and `SQLCODE` behaviour are not modelled
  beyond `SQLCODE 100` → 404.
- **The legacy path is a stand-in, not the mainframe.** `dropin/legacy` is a
  transliteration of `POLQRY`/`POLRNW` and can be wrong in the same way any
  reading can be wrong. Against a real region, the harness would drive the
  region instead — the comparison, and everything above it, would be unchanged.
- **Not the whole estate.** Inquiry and renewal only. `POLNEW`, `POLEND` and
  `UNDWRT` are pinned in the contract but not mirrored.
- **No performance, security or operational claim.** Nothing here says anything
  about throughput, failover or cutover sequencing.
