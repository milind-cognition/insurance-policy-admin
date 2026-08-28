package com.acme.dropin.contract;

import java.util.List;
import java.util.Optional;

/**
 * The three things a caller can ask the Policy Administration System to do.
 *
 * <p>This is the seam the whole demo turns on. The legacy path
 * ({@code dropin/legacy}, a transliteration of the COBOL) and the modern path
 * ({@code dropin/mirror}, Java 21 on an embedded database) both implement it,
 * and {@code dropin/switch} chooses between them at runtime. Nothing above this
 * interface - not the REST controller, not the .NET client - knows which one it
 * is talking to.
 */
public interface PasBackend {

    /** Which implementation this is; the value of {@code pas.backend}. */
    String name();

    /** {@code PQRY}/{@code POLQRY}: policy inquiry. Empty means DB2 SQLCODE 100. */
    Optional<PolicyView> findPolicy(String policyNumber);

    /** {@code PQRY}/{@code POLQRY}: the policy's coverages, ordered by sequence number, capped at 20. */
    List<CoverageView> findCoverages(String policyNumber);

    /** {@code PRWL}/{@code POLRNW}: renew the policy in place and return the new term. */
    RenewalResult renew(String policyNumber);
}
