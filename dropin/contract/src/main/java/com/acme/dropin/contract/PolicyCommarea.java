package com.acme.dropin.contract;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * The COMMAREA projection of a policy: {@link PolicyView} to and from the
 * {@code POLICY-RECORD} image a CICS program is handed.
 *
 * <p>One field genuinely loses information in this direction and it is worth
 * knowing about: the service boundary carries {@code lastUpdated} as an epoch
 * millisecond timestamp (the facade reads DB2 {@code LAST_UPDATED} with
 * {@code rs.getTimestamp}), while {@code POLICY-LAST-UPDATED} is
 * {@code PIC 9(08)}, a date. Round-tripping through the COMMAREA truncates the
 * time of day to midnight UTC.
 */
public final class PolicyCommarea {

    private static final CommareaCodec CODEC = new CommareaCodec(PasCopybooks.POLICY_RECORD);

    private PolicyCommarea() {
    }

    public static CommareaCodec codec() {
        return CODEC;
    }

    public static int recordLength() {
        return PasCopybooks.POLICY_RECORD.recordLength();
    }

    public static byte[] encode(PolicyView policy) {
        byte[] image = CODEC.newRecord();
        CODEC.putText(image, "POLICY-NUMBER", policy.policyNumber());
        CODEC.putText(image, "POLICY-TYPE", policy.policyType());
        CODEC.putText(image, "POLICY-STATUS", policy.policyStatus());
        CODEC.putDate(image, "POLICY-EFFECTIVE-DATE", policy.effectiveDate());
        CODEC.putDate(image, "POLICY-EXPIRY-DATE", policy.expiryDate());
        CODEC.putText(image, "POLICY-HOLDER-ID", policy.policyholderId());
        CODEC.putText(image, "POLICY-AGENT-CODE", policy.agentCode());
        CODEC.putText(image, "POLICY-BRANCH-CODE", policy.branchCode());
        CODEC.putDecimal(image, "POLICY-TOTAL-PREMIUM", policy.totalPremium());
        CODEC.putDecimal(image, "POLICY-DEDUCTIBLE", policy.deductible());
        CODEC.putDecimal(image, "POLICY-LIMIT", policy.coverageLimit());
        CODEC.putDate(image, "POLICY-INCEPTION-DATE", policy.inceptionDate());
        CODEC.putInteger(image, "POLICY-RENEWAL-COUNT", policy.renewalCount());
        CODEC.putText(image, "POLICY-UW-STATUS", policy.uwStatus());
        CODEC.putInteger(image, "POLICY-RISK-SCORE", policy.riskScore());
        CODEC.putText(image, "POLICY-WEB-IND", policy.webIndicator());
        CODEC.putText(image, "POLICY-API-FLAG", policy.apiFlag());
        CODEC.putDate(image, "POLICY-LAST-UPDATED", asDate(policy.lastUpdated()));
        CODEC.putText(image, "POLICY-UPDATED-BY", policy.updatedBy());
        return image;
    }

    public static PolicyView decode(byte[] image) {
        PasDate lastUpdated = CODEC.getDate(image, "POLICY-LAST-UPDATED");
        return new PolicyView(
                CODEC.getTrimmedText(image, "POLICY-NUMBER"),
                CODEC.getTrimmedText(image, "POLICY-TYPE"),
                CODEC.getTrimmedText(image, "POLICY-STATUS"),
                CODEC.getDate(image, "POLICY-EFFECTIVE-DATE"),
                CODEC.getDate(image, "POLICY-EXPIRY-DATE"),
                CODEC.getTrimmedText(image, "POLICY-HOLDER-ID"),
                CODEC.getTrimmedText(image, "POLICY-AGENT-CODE"),
                CODEC.getTrimmedText(image, "POLICY-BRANCH-CODE"),
                CODEC.getDecimal(image, "POLICY-TOTAL-PREMIUM"),
                CODEC.getDecimal(image, "POLICY-DEDUCTIBLE"),
                CODEC.getDecimal(image, "POLICY-LIMIT"),
                CODEC.getDate(image, "POLICY-INCEPTION-DATE"),
                (int) CODEC.getInteger(image, "POLICY-RENEWAL-COUNT"),
                CODEC.getTrimmedText(image, "POLICY-UW-STATUS"),
                (int) CODEC.getInteger(image, "POLICY-RISK-SCORE"),
                CODEC.getTrimmedText(image, "POLICY-WEB-IND"),
                CODEC.getTrimmedText(image, "POLICY-API-FLAG"),
                lastUpdated == null ? 0L
                        : lastUpdated.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                CODEC.getTrimmedText(image, "POLICY-UPDATED-BY"));
    }

    private static PasDate asDate(long epochMillis) {
        return epochMillis == 0 ? null
                : PasDate.of(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate());
    }
}
