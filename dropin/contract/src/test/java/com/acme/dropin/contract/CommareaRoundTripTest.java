package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The layer underneath the service boundary: the record image a CICS program is handed. */
class CommareaRoundTripTest {

    private final PolicyView policy = GoldenFixtures.policy("POL-00000001");

    @Test
    void policyRecordImageIsTheCopybookLength() {
        assertEquals(125, PolicyCommarea.encode(policy).length);
        assertEquals(124, CoverageCommarea.encode(GoldenFixtures.coverages("POL-00000001").get(0)).length);
    }

    @Test
    void textFieldsAreSpacePaddedToTheirPicXLength() {
        byte[] image = PolicyCommarea.encode(policy);
        // POLICY-TYPE is PIC X(03) and the value is "HOM"; POLICY-UPDATED-BY is PIC X(08).
        assertEquals("TNGUYEN ", PolicyCommarea.codec().getText(image, "POLICY-UPDATED-BY"));
        assertEquals("TNGUYEN", PolicyCommarea.codec().getTrimmedText(image, "POLICY-UPDATED-BY"));
    }

    @Test
    void datesAreEightDigitYyyymmdd() {
        byte[] image = PolicyCommarea.encode(policy);
        assertEquals("20250101", new String(image,
                PasCopybooks.POLICY_RECORD.field("POLICY-EFFECTIVE-DATE").offset(), 8,
                StandardCharsets.US_ASCII));
    }

    @Test
    void policyRoundTripsThroughTheCommareaExceptForTheTimeOfDay() {
        // POLICY-LAST-UPDATED is PIC 9(08), a date; the REST field is a timestamp.
        PolicyView decoded = PolicyCommarea.decode(PolicyCommarea.encode(policy));
        assertEquals(policy.withPremium(policy.totalPremium()).toString().replaceAll("lastUpdated=\\d+", ""),
                decoded.toString().replaceAll("lastUpdated=\\d+", ""));
    }

    @Test
    void coverageRoundTripsThroughTheCommarea() {
        for (CoverageView coverage : GoldenFixtures.coverages("POL-00000001")) {
            assertEquals(coverage, CoverageCommarea.decode(CoverageCommarea.encode(coverage)));
        }
    }
}
