package com.acme.dropin.parity;

import com.acme.dropin.contract.ContractJson;
import com.acme.dropin.contract.ContractXml;
import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PasBackend;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;
import com.acme.dropin.legacy.LegacyPolicyAdministration;
import com.acme.dropin.mirror.MirrorDatabase;
import com.acme.dropin.mirror.MirrorPolicyAdministration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Drives the legacy path and the mirror with the same population and compares
 * what comes back, byte for byte, at the service boundary.
 *
 * <p>The comparison is deliberately made on the serialised document rather than
 * on Java objects: a caller sees bytes, so bytes are what has to match. Both
 * sides are driven by the same clock so that {@code lastUpdated} is not a
 * source of noise - a real difference has to be a real difference.
 */
public final class ParityHarness {

    /** Fixed so the run is reproducible and audit timestamps cannot differ by accident. */
    public static final Instant DEMO_INSTANT = Instant.parse("2026-03-01T00:00:00Z");

    private final ObjectMapper json = ContractJson.mapper();

    public ParityReport run() {
        Clock clock = Clock.fixed(DEMO_INSTANT, ZoneOffset.UTC);
        PasBackend mainframe = LegacyPolicyAdministration.loadedWithDemoData(clock);
        PasBackend mirror = new MirrorPolicyAdministration(
                MirrorDatabase.of("jdbc:h2:mem:pasparity;DB_CLOSE_DELAY=-1;MODE=DB2").loadDemoData(), clock);

        List<ParityDifference> differences = new ArrayList<>();
        int cases = 0;

        for (PolicyView seed : DemoDataset.policies()) {
            String policyNumber = seed.policyNumber();
            cases += compare(policyNumber, "inquiry",
                    () -> policyDocument(mainframe.findPolicy(policyNumber)),
                    () -> policyDocument(mirror.findPolicy(policyNumber)),
                    differences);

            cases += compare(policyNumber, "inquiry-xml",
                    () -> mainframe.findPolicy(policyNumber).map(ContractXml::write).orElse("<absent/>"),
                    () -> mirror.findPolicy(policyNumber).map(ContractXml::write).orElse("<absent/>"),
                    differences);

            cases += compare(policyNumber, "coverages",
                    () -> coverageDocument(mainframe.findCoverages(policyNumber)),
                    () -> coverageDocument(mirror.findCoverages(policyNumber)),
                    differences);

            cases += compare(policyNumber, "renewal",
                    () -> renewalDocument(mainframe.renew(policyNumber)),
                    () -> renewalDocument(mirror.renew(policyNumber)),
                    differences);
        }

        cases += compare("PAS-99999999", "inquiry",
                () -> policyDocument(mainframe.findPolicy("PAS-99999999")),
                () -> policyDocument(mirror.findPolicy("PAS-99999999")),
                differences);

        return new ParityReport(cases, differences);
    }

    private int compare(String policyNumber, String operation,
                        Document mainframe, Document mirror,
                        List<ParityDifference> differences) {
        String expected = mainframe.render();
        String actual = mirror.render();
        if (!expected.equals(actual)) {
            differences.add(new ParityDifference(policyNumber, operation, expected, actual));
        }
        return 1;
    }

    private String policyDocument(Optional<PolicyView> policy) {
        return policy.map(this::write).orElse("404 POLICY NOT FOUND");
    }

    private String coverageDocument(List<CoverageView> coverages) {
        return write(coverages);
    }

    private String renewalDocument(RenewalResult result) {
        return result.succeeded() ? write(result.policy()) : "409 " + result.errorMessage();
    }

    private String write(Object document) {
        try {
            return json.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise a parity document", e);
        }
    }

    @FunctionalInterface
    private interface Document {
        String render();
    }
}
