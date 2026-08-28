package com.acme.dropin.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates the OpenAPI 3 description of the caller-visible service boundary
 * from the incumbent facade's own source.
 *
 * <p>The generated document is committed at
 * {@code dropin/contract/src/main/resources/openapi/pas-v1.yaml} and
 * {@code OpenApiGeneratorTest} regenerates it and fails on any difference, so
 * a change to the incumbent controller or its models cannot silently change
 * what callers were promised.
 *
 * <p>Every response schema carries {@code x-cics-*} extensions tying the JSON
 * field back to the COMMAREA field underneath it, which is how a caller on the
 * service boundary and a program on the COMMAREA boundary stay in agreement.
 */
public final class OpenApiGenerator {

    private final FacadeSource facade;
    private final Copybook policyCopybook;
    private final Copybook coverageCopybook;

    public OpenApiGenerator(FacadeSource facade, Copybook policyCopybook, Copybook coverageCopybook) {
        this.facade = facade;
        this.policyCopybook = policyCopybook;
        this.coverageCopybook = coverageCopybook;
    }

    public static OpenApiGenerator fromRepository() {
        return new OpenApiGenerator(
                new FacadeSource(),
                Copybook.parse(RepoLayout.copybook("POLICY-RECORD.cpy")),
                Copybook.parse(RepoLayout.copybook("COVERAGE-RECORD.cpy")));
    }

    public static void main(String[] args) throws IOException {
        Path out = args.length > 0
                ? Path.of(args[0])
                : RepoLayout.repoRoot().resolve("dropin/contract/src/main/resources/openapi/pas-v1.yaml");
        Files.createDirectories(out.getParent());
        Files.writeString(out, fromRepository().generate());
        System.out.println("wrote " + out);
    }

    public String generate() {
        StringBuilder y = new StringBuilder();
        y.append("""
                # GENERATED FILE - do not edit by hand.
                # Produced by com.acme.dropin.contract.OpenApiGenerator from:
                #   java-facade/src/main/java/com/acme/insurance/pas/controller/PolicyController.java
                #   java-facade/src/main/java/com/acme/insurance/pas/model/{Policy,Coverage}.java
                #   java-facade/src/main/java/com/acme/insurance/pas/repository/PolicyRepository.java
                #   cobol/copybooks/{POLICY-RECORD,COVERAGE-RECORD}.cpy
                # Regenerate with: mvn -pl contract exec:java -Dexec.mainClass=com.acme.dropin.contract.OpenApiGenerator
                openapi: 3.0.3
                info:
                  title: Policy Administration System - caller-visible service contract
                  version: 1.0.0
                  description: >-
                    The service boundary that .NET and Java callers integrate against. Requests and
                    responses are carried as JSON here and as the equivalent XML documents over CICS
                    Web Services; both projections describe the same fields in the same order. The
                    COMMAREA layouts documented under x-cics-commarea are the layer underneath this
                    boundary, not the boundary itself.
                servers:
                  - url: http://localhost:8080
                    description: Local demo, either backend (pas.backend=mainframe|mirror)
                paths:
                """);

        for (FacadeSource.Operation op : facade.operations()) {
            boolean list = op.responseType().startsWith("List<");
            String schemaRef = list
                    ? "$ref: '#/components/schemas/CoverageList'"
                    : "$ref: '#/components/schemas/Policy'";
            String okDescription = list
                    ? "Coverages for the policy, ordered by SEQUENCE_NUM as POLQRY reads them"
                    : "Policy found";
            y.append("  ").append(op.path()).append(":\n");
            y.append("    get:\n");
            y.append("      operationId: ").append(op.operationId()).append('\n');
            y.append("      x-cics-transaction: PQRY\n");
            y.append("      x-cics-program: POLQRY\n");
            y.append("""
                          parameters:
                            - name: policyNumber
                              in: path
                              required: true
                              description: Policy number, PIC X(12) on the COMMAREA (space padded there, trimmed here)
                              schema:
                                type: string
                                maxLength: 12
                          responses:
                            '200':
                              description: %s
                              content:
                                application/json:
                                  schema:
                                    %s
                                application/xml:
                                  schema:
                                    %s
                    """.formatted(okDescription, schemaRef, schemaRef));
            if (op.returnsNotFound()) {
                y.append("""
                                '404':
                                  description: >-
                                    No policy with that number. Empty body. Corresponds to DB2
                                    SQLCODE 100 and to the 'POLICY NOT FOUND' path in POLQRY.cbl.
                        """);
            }
        }

        y.append("components:\n  schemas:\n");
        y.append("    CoverageList:\n      type: array\n      xml:\n        name: coverages\n        wrapped: true\n");
        y.append("      items:\n        $ref: '#/components/schemas/Coverage'\n");
        appendSchema(y, "Policy", "policy", facade.policyFields(), policyCopybook, "POLICY-RECORD");
        appendSchema(y, "Coverage", "coverage", facade.coverageFields(), coverageCopybook, "COVERAGE-RECORD");
        return y.toString();
    }

    private void appendSchema(StringBuilder y, String name, String xmlName,
                              List<FacadeSource.ModelField> fields, Copybook copybook, String recordName) {
        y.append("    ").append(name).append(":\n");
        y.append("      type: object\n");
        y.append("      xml:\n        name: ").append(xmlName).append('\n');
        y.append("      x-cics-commarea:\n");
        y.append("        copybook: cobol/copybooks/").append(recordName).append(".cpy\n");
        y.append("        record: ").append(copybook.recordName()).append('\n');
        y.append("        length: ").append(copybook.recordLength()).append('\n');
        y.append("      properties:\n");
        for (FacadeSource.ModelField f : fields) {
            CopybookField cf = CommareaMapping.copybookFieldFor(name, f.name(), copybook);
            y.append("        ").append(f.name()).append(":\n");
            for (String line : jsonType(f)) {
                y.append("          ").append(line).append('\n');
            }
            if (cf != null) {
                y.append("          x-cobol-field: ").append(cf.name()).append('\n');
                y.append("          x-cobol-picture: ").append(cf.pic()).append('\n');
                y.append("          x-cobol-offset: ").append(cf.offset()).append('\n');
                y.append("          x-cobol-length: ").append(cf.length()).append('\n');
            }
        }
    }

    /**
     * JSON type as the incumbent facade actually emits it, verified against a
     * running instance: {@code rs.getDate} fields serialize as "YYYY-MM-DD",
     * {@code rs.getTimestamp} fields as epoch milliseconds, BigDecimal columns
     * keep their DB2 scale (2) and are emitted unquoted.
     */
    private List<String> jsonType(FacadeSource.ModelField f) {
        String accessor = f.jdbcAccessor() == null ? "" : f.jdbcAccessor();
        return switch (f.javaType()) {
            case "String" -> List.of("type: string");
            case "int", "Integer" -> List.of("type: integer", "format: int32");
            case "BigDecimal" -> List.of("type: number", "format: decimal", "x-decimal-scale: 2");
            case "Date" -> accessor.equals("Timestamp")
                    ? List.of("type: integer", "format: int64",
                              "description: Epoch milliseconds; the facade maps this column with rs.getTimestamp")
                    : List.of("type: string", "format: date");
            default -> throw new IllegalStateException(
                    "Unmapped facade field type " + f.javaType() + " on " + f.name());
        };
    }
}
