package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the contract and the incumbent drifting apart.
 *
 * <p>Nothing here tests the drop-in machinery; it tests that the documents in
 * this module still describe the application in {@code java-facade} and the
 * copybooks in {@code cobol/copybooks}, both of which this work is forbidden
 * to modify.
 */
class ContractDriftTest {

    private final FacadeSource facade = new FacadeSource();

    @Test
    void openApiDocumentMatchesTheGeneratorOutput() throws IOException {
        Path committed = RepoLayout.repoRoot()
                .resolve("dropin/contract/src/main/resources/openapi/pas-v1.yaml");
        assertEquals(Files.readString(committed), OpenApiGenerator.fromRepository().generate(),
                "pas-v1.yaml is stale; regenerate with "
                        + "mvn -pl contract exec:java -Dexec.mainClass=com.acme.dropin.contract.OpenApiGenerator");
    }

    @Test
    void facadeStillExposesTheTwoDocumentedOperations() {
        List<FacadeSource.Operation> operations = facade.operations();
        assertEquals(2, operations.size());
        assertEquals("/api/v1/policies/{policyNumber}", operations.get(0).path());
        assertEquals("/api/v1/policies/{policyNumber}/coverages", operations.get(1).path());
        assertTrue(operations.stream().allMatch(FacadeSource.Operation::returnsNotFound));
    }

    @Test
    void everyRestFieldHasACommareaFieldUnderneathIt() {
        for (FacadeSource.ModelField field : facade.policyFields()) {
            String cobol = CommareaMapping.POLICY.get(field.name());
            assertNotNull(cobol, "No COMMAREA field mapped for policy." + field.name());
            assertNotNull(PasCopybooks.POLICY_RECORD.field(cobol));
        }
        for (FacadeSource.ModelField field : facade.coverageFields()) {
            String cobol = CommareaMapping.COVERAGE.get(field.name());
            assertNotNull(cobol, "No COMMAREA field mapped for coverage." + field.name());
            assertNotNull(PasCopybooks.COVERAGE_RECORD.field(cobol));
        }
    }

    @Test
    void contractRecordsCarryExactlyTheFacadeFieldsInOrder() {
        assertEquals(facade.policyFields().stream().map(FacadeSource.ModelField::name).toList(),
                java.util.Arrays.stream(PolicyView.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).toList());
        assertEquals(facade.coverageFields().stream().map(FacadeSource.ModelField::name).toList(),
                java.util.Arrays.stream(CoverageView.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).toList());
    }
}
