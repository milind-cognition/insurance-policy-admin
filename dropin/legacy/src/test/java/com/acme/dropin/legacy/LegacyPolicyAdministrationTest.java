package com.acme.dropin.legacy;

import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PasDate;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPolicyAdministrationTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.ofEpochMilli(DemoDataset.FIXED_LAST_UPDATED), ZoneOffset.UTC);

    private LegacyPolicyAdministration legacy;

    @BeforeEach
    void setUp() {
        legacy = LegacyPolicyAdministration.loadedWithDemoData(FIXED);
    }

    @Test
    void inquiryReturnsTheStoredRecordAndNothingForAnUnknownNumber() {
        PolicyView policy = legacy.findPolicy("PAS-00000001").orElseThrow();
        assertEquals("PAS-00000001", policy.policyNumber());
        assertEquals(new BigDecimal("501.37"), policy.totalPremium());
        assertTrue(legacy.findPolicy("PAS-99999999").isEmpty(), "SQLCODE 100 is an empty result");
    }

    @Test
    void coveragesComeBackInSequenceOrder() {
        List<CoverageView> coverages = legacy.findCoverages("PAS-00000001");
        assertFalse(coverages.isEmpty());
        for (int i = 0; i < coverages.size(); i++) {
            assertEquals(i + 1, coverages.get(i).sequenceNum());
        }
    }

    @Test
    void renewalTruncatesThePremiumRatherThanRoundingIt() {
        // 501.37 * 1.05 = 526.4385 -> COBOL stores 526.43, not 526.44.
        PolicyView renewed = legacy.renew("PAS-00000001").policy();
        assertEquals(new BigDecimal("526.43"), renewed.totalPremium());
        assertEquals("AC", renewed.policyStatus());
        assertEquals("PN", renewed.uwStatus());
        assertEquals("POLRNW", renewed.updatedBy());
    }

    @Test
    void renewalMovesTheTermForwardWithDateArithmetic() {
        PolicyView before = legacy.findPolicy("PAS-00000003").orElseThrow();
        assertEquals(PasDate.of(20240229), before.expiryDate(), "the leap-day term");

        PolicyView renewed = legacy.renew("PAS-00000003").policy();
        assertEquals(PasDate.of(20240229), renewed.effectiveDate());
        assertEquals(PasDate.of(20250229), renewed.expiryDate());
        assertFalse(renewed.expiryDate().isRealCalendarDate(),
                "POLRNW writes a 29 February into a year that has none");
        assertEquals(before.renewalCount() + 1, renewed.renewalCount());
    }

    @Test
    void renewedCoveragesFollowTheNewTerm() {
        PolicyView renewed = legacy.renew("PAS-00000002").policy();
        for (CoverageView coverage : legacy.findCoverages("PAS-00000002")) {
            assertEquals(renewed.effectiveDate(), coverage.effectiveDate());
            assertEquals(renewed.expiryDate(), coverage.expiryDate());
        }
    }

    @Test
    void onlyActiveAndExpiredPoliciesRenew() {
        assertTrue(legacy.renew("PAS-00000007").succeeded(), "EX is renewable");
        assertEquals(RenewalResult.NOT_ELIGIBLE, legacy.renew("PAS-00000008").errorMessage());
        assertEquals(RenewalResult.NOT_ELIGIBLE, legacy.renew("PAS-00000009").errorMessage(),
                "LP is not renewable");
        assertEquals(RenewalResult.NOT_FOUND, legacy.renew("PAS-99999999").errorMessage());
    }
}
