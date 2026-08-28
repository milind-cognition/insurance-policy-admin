package com.acme.dropin.contract;

/** The COMMAREA projection of a coverage: {@link CoverageView} to and from {@code COVERAGE-RECORD}. */
public final class CoverageCommarea {

    private static final CommareaCodec CODEC = new CommareaCodec(PasCopybooks.COVERAGE_RECORD);

    private CoverageCommarea() {
    }

    public static CommareaCodec codec() {
        return CODEC;
    }

    public static int recordLength() {
        return PasCopybooks.COVERAGE_RECORD.recordLength();
    }

    public static byte[] encode(CoverageView coverage) {
        byte[] image = CODEC.newRecord();
        CODEC.putText(image, "COV-POLICY-NUMBER", coverage.policyNumber());
        CODEC.putInteger(image, "COV-SEQUENCE-NUM", coverage.sequenceNum());
        CODEC.putText(image, "COV-TYPE-CODE", coverage.coverageType());
        CODEC.putText(image, "COV-DESCRIPTION", coverage.description());
        CODEC.putDecimal(image, "COV-LIMIT", coverage.coverageLimit());
        CODEC.putDecimal(image, "COV-DEDUCTIBLE", coverage.deductible());
        CODEC.putDecimal(image, "COV-PREMIUM", coverage.premium());
        CODEC.putDate(image, "COV-EFFECTIVE-DATE", coverage.effectiveDate());
        CODEC.putDate(image, "COV-EXPIRY-DATE", coverage.expiryDate());
        CODEC.putText(image, "COV-STATUS", coverage.status());
        CODEC.putInteger(image, "COV-COINSURANCE-PCT", coverage.coinsurancePct());
        CODEC.putText(image, "COV-RATING-TERRITORY", coverage.ratingTerritory());
        CODEC.putText(image, "COV-CLASS-CODE", coverage.classCode());
        return image;
    }

    public static CoverageView decode(byte[] image) {
        return new CoverageView(
                CODEC.getTrimmedText(image, "COV-POLICY-NUMBER"),
                (int) CODEC.getInteger(image, "COV-SEQUENCE-NUM"),
                CODEC.getTrimmedText(image, "COV-TYPE-CODE"),
                CODEC.getTrimmedText(image, "COV-DESCRIPTION"),
                CODEC.getDecimal(image, "COV-LIMIT"),
                CODEC.getDecimal(image, "COV-DEDUCTIBLE"),
                CODEC.getDecimal(image, "COV-PREMIUM"),
                CODEC.getDate(image, "COV-EFFECTIVE-DATE"),
                CODEC.getDate(image, "COV-EXPIRY-DATE"),
                CODEC.getTrimmedText(image, "COV-STATUS"),
                (int) CODEC.getInteger(image, "COV-COINSURANCE-PCT"),
                CODEC.getTrimmedText(image, "COV-RATING-TERRITORY"),
                CODEC.getTrimmedText(image, "COV-CLASS-CODE"));
    }
}
