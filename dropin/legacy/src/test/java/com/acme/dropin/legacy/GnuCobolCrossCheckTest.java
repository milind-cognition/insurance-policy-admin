package com.acme.dropin.legacy;

import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PasDate;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RepoLayout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks the Java transliteration against an actual COBOL compiler.
 *
 * <p>{@link CobolArithmetic} claims that {@code COMPUTE} truncates, that
 * {@code PIC 9(03)} wraps and that {@code ADD 10000} to a date produces
 * 2025-02-29. Those are claims about a language, and the cheapest way to stop
 * being wrong about them is to compile the arithmetic with GnuCOBOL and run
 * both.
 *
 * <p>Skipped when {@code cobc} is not installed, so a clean clone still builds.
 */
class GnuCobolCrossCheckTest {

    private static Path binary;

    @BeforeAll
    static void compileWithGnuCobol() throws Exception {
        assumeTrue(onPath("cobc"), "GnuCOBOL (cobc) not installed - skipping the cross-check");
        Path source = RepoLayout.repoRoot().resolve("dropin/legacy/src/main/cobol/RNWCALC.cbl");
        Path target = Files.createTempDirectory("rnwcalc").resolve("rnwcalc");
        Process cobc = new ProcessBuilder("cobc", "-x", "-o", target.toString(), source.toString())
                .redirectErrorStream(true).start();
        String output = new String(cobc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, cobc.waitFor(), "cobc failed: " + output);
        binary = target;
    }

    @Test
    void everyRenewalMatchesWhatTheCompiledCobolProduces() throws Exception {
        List<PolicyView> sample = DemoDataset.policies().stream()
                .filter(p -> "AC".equals(p.policyStatus()) || "EX".equals(p.policyStatus()))
                .limit(60)
                .toList();
        for (PolicyView policy : sample) {
            String[] cobol = run(policy.totalPremium(), policy.expiryDate(), policy.renewalCount());

            assertEquals(new BigDecimal(cobol[0]),
                    CobolArithmetic.applyRateIncrease(policy.totalPremium()),
                    policy.policyNumber() + " premium");
            assertEquals(PasDate.of(Integer.parseInt(cobol[1])),
                    policy.expiryDate().plusYearsByArithmetic(1),
                    policy.policyNumber() + " expiry");
            assertEquals(Integer.parseInt(cobol[2]),
                    CobolArithmetic.addToPic9(policy.renewalCount(), 1, 3),
                    policy.policyNumber() + " renewal count");
            assertEquals(new BigDecimal(cobol[3]),
                    CobolArithmetic.rateChangePercent(policy.totalPremium(),
                            CobolArithmetic.applyRateIncrease(policy.totalPremium())),
                    policy.policyNumber() + " rate change percent");
        }
    }

    @Test
    void theTruncationAndTheLeapDayAreRealCobolBehaviour() throws Exception {
        // 1249.99 * 1.05 = 1312.4895; COBOL stores 1312.48, a rewrite that rounds gets 1312.49.
        assertEquals("1312.48", run(new BigDecimal("1249.99"), PasDate.of(20250101), 0)[0]);
        // A term expiring on a leap day renews into a day that does not exist.
        assertEquals("20250229", run(new BigDecimal("1000.00"), PasDate.of(20240229), 0)[1]);
        // PIC 9(03) has no room for 1000.
        assertEquals("000", run(new BigDecimal("1000.00"), PasDate.of(20250101), 999)[2]);
    }

    /** @return premium, expiry, renewal count, rate change percent - as the COBOL prints them. */
    private static String[] run(BigDecimal premium, PasDate expiry, int renewalCount) throws Exception {
        Process process = new ProcessBuilder(binary.toString()).redirectErrorStream(true).start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(String.format("%s%n%08d%n%03d%n", premium.toPlainString(),
                    expiry.yyyymmdd(), renewalCount).getBytes(StandardCharsets.UTF_8));
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("RNWCALC did not terminate");
        }
        String[] parts = output.split("\\|");
        // The COBOL emits zero-padded edited fields; strip the padding, keep the value.
        parts[0] = new BigDecimal(parts[0]).toPlainString();
        parts[3] = new BigDecimal(parts[3]).toPlainString();
        return parts;
    }

    private static boolean onPath(String command) throws IOException {
        try {
            return new ProcessBuilder("which", command).start().waitFor() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
