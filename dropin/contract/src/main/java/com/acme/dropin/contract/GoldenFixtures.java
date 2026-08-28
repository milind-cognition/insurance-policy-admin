package com.acme.dropin.contract;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Responses captured from the incumbent facade itself.
 *
 * <p>Recorded by {@code dropin/scripts/capture-golden-fixtures.sh} against the
 * unmodified Java 8 / Spring Boot 1.5 application on its own H2 profile, so
 * these bytes are what a caller sees today, not what this repository believes a
 * caller sees. {@code lastUpdated} is normalised to 0 because the seed data
 * sets it to {@code CURRENT_TIMESTAMP}.
 */
public final class GoldenFixtures {

    public static final List<String> POLICY_NUMBERS =
            List.of("POL-00000001", "POL-00000002", "POL-00000003");

    private GoldenFixtures() {
    }

    public static String policyJson(String policyNumber) {
        return read("/golden/" + policyNumber + ".policy.json");
    }

    public static String coveragesJson(String policyNumber) {
        return read("/golden/" + policyNumber + ".coverages.json");
    }

    public static PolicyView policy(String policyNumber) {
        return ContractJson.read(policyJson(policyNumber), PolicyView.class);
    }

    public static List<CoverageView> coverages(String policyNumber) {
        try {
            return ContractJson.mapper().readValue(coveragesJson(policyNumber),
                    new TypeReference<List<CoverageView>>() {
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read coverage fixture for " + policyNumber, e);
        }
    }

    /** HTTP status the incumbent returns for a policy number that does not exist. */
    public static int unknownPolicyStatus() {
        return Integer.parseInt(read("/golden/unknown-policy.status").trim());
    }

    private static String read(String resource) {
        try (InputStream in = GoldenFixtures.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing golden fixture " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }
}
