package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** COMP-3 is two digits per byte with a sign nibble; the hex is checkable by hand. */
class PackedDecimalTest {

    private final PicClause premium = PasCopybooks.POLICY_RECORD.field("POLICY-TOTAL-PREMIUM").pic();

    @Test
    void encodesPositiveAmountWithTrailingSignNibble() {
        // 1250.00 as 11 digits, scale 2 -> 00000125000 C
        assertEquals("00000125000C", PackedDecimal.hex(PackedDecimal.encode(new BigDecimal("1250.00"), premium)));
    }

    @Test
    void encodesNegativeAmountWithDNibble() {
        assertEquals("00000125000D", PackedDecimal.hex(PackedDecimal.encode(new BigDecimal("-1250.00"), premium)));
    }

    @Test
    void roundTripsThroughTheRecordImage() {
        for (String amount : new String[]{"0.00", "0.01", "890.50", "999999999.99", "-42.42"}) {
            byte[] packed = PackedDecimal.encode(new BigDecimal(amount), premium);
            assertEquals(new BigDecimal(amount), PackedDecimal.decode(packed, 0, premium));
        }
    }

    @Test
    void truncatesRatherThanRoundsIntoAV99Field() {
        // COBOL COMPUTE into PIC S9(9)V99 drops the excess digits.
        byte[] packed = PackedDecimal.encode(new BigDecimal("1312.4895"), premium);
        assertEquals(new BigDecimal("1312.48"), PackedDecimal.decode(packed, 0, premium));
    }
}
