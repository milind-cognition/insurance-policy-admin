package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The COMMAREA layout is read from the copybooks the CICS programs compile against. */
class CopybookTest {

    private final Copybook policy = PasCopybooks.POLICY_RECORD;
    private final Copybook coverage = PasCopybooks.COVERAGE_RECORD;

    @Test
    void parsesPolicyRecordInDeclaredOrder() {
        assertEquals("POLICY-RECORD", policy.recordName());
        assertEquals("POLICY-NUMBER", policy.fields().get(0).name());
        assertEquals(0, policy.field("POLICY-NUMBER").offset());
        assertEquals(12, policy.field("POLICY-NUMBER").length());
        assertEquals(12, policy.field("POLICY-TYPE").offset());
        assertTrue(policy.fields().get(policy.fields().size() - 1).isFiller());
    }

    @Test
    void sizesPackedDecimalFieldsTwoDigitsPerByteWithASignNibble() {
        // PIC S9(09)V99 COMP-3 is 11 digits -> 11/2 + 1 = 6 bytes.
        assertEquals(6, policy.field("POLICY-TOTAL-PREMIUM").length());
        assertEquals(5, policy.field("POLICY-DEDUCTIBLE").length());
        assertEquals(7, policy.field("POLICY-LIMIT").length());
        assertEquals(PicClause.Usage.COMP_3, policy.field("POLICY-LIMIT").pic().usage());
        assertEquals(2, policy.field("POLICY-LIMIT").pic().decimalDigits());
    }

    @Test
    void computesRecordLengthsIncludingFiller() {
        assertEquals(125, policy.recordLength());
        assertEquals(124, coverage.recordLength());
        // Both fit the PIC X(256) COMMAREA the CICS programs declare.
        assertTrue(policy.recordLength() <= 256);
        assertTrue(coverage.recordLength() <= 256);
    }

    @Test
    void keepsFourDecimalRatingFactorsDistinctFromTwoDecimalMoney() {
        Copybook premium = PasCopybooks.PREMIUM_RECORD;
        assertEquals(4, premium.field("PREM-BASE-RATE").pic().decimalDigits());
        assertEquals(2, premium.field("PREM-TOTAL-PREMIUM").pic().decimalDigits());
    }
}
