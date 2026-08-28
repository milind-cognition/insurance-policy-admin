package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The contract types reproduce the incumbent's bytes.
 *
 * <p>Each fixture is parsed into {@link PolicyView} / {@link CoverageView} and
 * serialized again; the result has to be identical to what the running Spring
 * Boot 1.5 application returned. If a modern Jackson default would render
 * {@code 1250.00} as {@code 1250.0} or a date as an epoch number, this fails.
 */
class GoldenFixtureTest {

    @Test
    void policyResponsesRoundTripByteForByte() {
        for (String policyNumber : GoldenFixtures.POLICY_NUMBERS) {
            String captured = GoldenFixtures.policyJson(policyNumber);
            assertEquals(captured, ContractJson.write(ContractJson.read(captured, PolicyView.class)),
                    policyNumber + " policy body differs from the incumbent");
        }
    }

    @Test
    void coverageResponsesRoundTripByteForByte() {
        for (String policyNumber : GoldenFixtures.POLICY_NUMBERS) {
            String captured = GoldenFixtures.coveragesJson(policyNumber);
            assertEquals(captured, ContractJson.write(GoldenFixtures.coverages(policyNumber)),
                    policyNumber + " coverage body differs from the incumbent");
        }
    }

    @Test
    void capturedValuesAreTheOnesTheDemoTalksAbout() {
        PolicyView policy = GoldenFixtures.policy("POL-00000001");
        assertEquals(new BigDecimal("1250.00"), policy.totalPremium());
        assertEquals(PasDate.of(2026, 1, 1), policy.expiryDate());
        assertEquals(2, GoldenFixtures.coverages("POL-00000001").size());
    }

    @Test
    void unknownPolicyIsNotFoundWithAnEmptyBody() {
        assertEquals(404, GoldenFixtures.unknownPolicyStatus());
    }

    @Test
    void coveragesAreOrderedBySequenceNumber() {
        List<CoverageView> coverages = GoldenFixtures.coverages("POL-00000001");
        for (int i = 0; i < coverages.size(); i++) {
            assertEquals(i + 1, coverages.get(i).sequenceNum());
        }
    }
}
