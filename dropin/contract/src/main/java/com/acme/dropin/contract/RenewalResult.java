package com.acme.dropin.contract;

/**
 * What {@code POLRNW} hands back: either the renewed policy record, or the text
 * it puts in {@code WS-ERROR-MSG} and sends with {@code EXEC CICS SEND TEXT}.
 *
 * <p>The error strings are the COBOL literals verbatim, because a caller today
 * sees those bytes and some of them match on them.
 */
public record RenewalResult(PolicyView policy, String errorMessage) {

    /** {@code MOVE 'POLICY NOT FOUND FOR RENEWAL' TO WS-ERROR-MSG} - POLRNW 2000-READ-EXISTING-POLICY. */
    public static final String NOT_FOUND = "POLICY NOT FOUND FOR RENEWAL";

    /** {@code MOVE 'POLICY NOT ELIGIBLE FOR RENEWAL' TO WS-ERROR-MSG} - POLRNW 3000-CHECK-RENEWAL-ELIGIBILITY. */
    public static final String NOT_ELIGIBLE = "POLICY NOT ELIGIBLE FOR RENEWAL";

    public static RenewalResult renewed(PolicyView policy) {
        return new RenewalResult(policy, null);
    }

    public static RenewalResult failed(String errorMessage) {
        return new RenewalResult(null, errorMessage);
    }

    public boolean succeeded() {
        return errorMessage == null;
    }
}
