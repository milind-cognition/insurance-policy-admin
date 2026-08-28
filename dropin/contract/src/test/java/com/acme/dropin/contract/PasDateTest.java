package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date type has to be able to hold what POLRNW produces, including the
 * value that is not a date.
 */
class PasDateTest {

    @Test
    void addTenThousandIsWhatPolrnwDoes() {
        assertEquals(PasDate.of(20260101), PasDate.of(20250101).plusYearsByArithmetic(1));
        assertEquals(PasDate.of(20250229), PasDate.of(20240229).plusYearsByArithmetic(1));
    }

    @Test
    void twentyTwentyFiveHasNoTwentyNinthOfFebruary() {
        PasDate renewed = PasDate.of(20240229).plusYearsByArithmetic(1);
        assertFalse(renewed.isRealCalendarDate());
        assertEquals("2025-02-29", renewed.toString());
        assertTrue(PasDate.of(20240229).isRealCalendarDate());
    }

    @Test
    void aCalendarAwareImplementationLandsOnADifferentDay() {
        assertEquals(PasDate.of(20250228), PasDate.of(20240229).plusYearsByCalendar(1));
    }

    @Test
    void serializesAsTheIsoStringTheFacadeEmits() {
        assertEquals("\"2025-02-29\"", ContractJson.write(PasDate.of(20250229)));
        assertEquals(PasDate.of(20250229), ContractJson.read("\"2025-02-29\"", PasDate.class));
    }
}
