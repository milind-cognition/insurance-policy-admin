package com.acme.dropin.parity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate: every case in the population has to come back byte-identical from
 * both implementations before the mirror can be called a drop-in replacement.
 *
 * <p>It fails today, on purpose, and the failure is the point of the demo - the
 * unit tests in every other module are green while three real differences sit
 * in the responses. Run it with {@code make parity}; a normal {@code mvn test}
 * skips it (see this module's surefire configuration) so the build stays honest
 * about what is a unit test and what is a release gate.
 */
class ParityTest {

    @Test
    void the_mirror_answers_identically_to_the_mainframe_path() {
        ParityReport report = new ParityHarness().run();
        assertTrue(report.clean(), System.lineSeparator() + report.render());
    }
}
