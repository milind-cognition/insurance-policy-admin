package com.acme.dropin.parity;

/**
 * {@code make parity}. Prints {@code N/M cases identical} and exits non-zero if
 * anything differs, so it can be wired in as a release gate rather than read.
 */
public final class ParityMain {

    public static void main(String[] args) {
        ParityReport report = new ParityHarness().run();
        System.out.print(report.render());
        if (!report.clean()) {
            System.exit(1);
        }
    }

    private ParityMain() {
    }
}
