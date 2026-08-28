package com.acme.dropin.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The copybooks the CICS programs compile against, parsed once.
 *
 * <p>They are read from the classpath, where the build copies them straight out
 * of {@code cobol/copybooks} - the contract cannot drift from the copybook
 * because it does not keep its own copy of it.
 */
public final class PasCopybooks {

    public static final Copybook POLICY_RECORD = load("POLICY-RECORD.cpy");
    public static final Copybook COVERAGE_RECORD = load("COVERAGE-RECORD.cpy");
    public static final Copybook PREMIUM_RECORD = load("PREMIUM-RECORD.cpy");

    private PasCopybooks() {
    }

    public static Copybook load(String fileName) {
        String resource = "/copybooks/" + fileName;
        try (InputStream in = PasCopybooks.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Copybook " + resource + " is not on the classpath; "
                        + "run the build so it is copied from cobol/copybooks");
            }
            return Copybook.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }
}
