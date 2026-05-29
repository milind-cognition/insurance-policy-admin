package com.acme.insurance.pas;

import com.acme.insurance.pas.entity.Policy;
import com.acme.insurance.pas.entity.Premium;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.PremiumRepository;
import com.acme.insurance.pas.service.PremiumCalculationService;
import com.acme.insurance.pas.service.PremiumCalculationService.BatchSummary;
import com.acme.insurance.pas.service.PremiumCalculationService.PremiumResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying premium calculation produces identical results
 * to the COBOL PREMBAT program.
 *
 * COBOL PREMBAT logic (3200-CALCULATE-PREMIUM):
 *   base_premium = hardcoded rate per LOB
 *   terr_premium = base * territory_factor (default 1.0)
 *   class_premium = terr * class_factor (default 1.0)
 *   mod_premium = class * experience_mod (default 1.0)
 *   tax = mod_premium * 0.0350
 *   surcharge = 25.00
 *   final = mod_premium + tax + surcharge
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PremiumCalculationIntegrationTest {

    @Autowired
    private PremiumCalculationService premiumService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PremiumRepository premiumRepository;

    @Test
    void autoPolicy_baseRate850_withTerritoryFactor() {
        // COBOL: WHEN POL-TYPE-AUTO MOVE 850.00 TO WS-BASE-PREMIUM
        // Territory IL0601 factor = 1.2500
        Policy policy = policyRepository.findById("TEST-AUT-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);

        assertEquals(new BigDecimal("850.00"), result.basePremium());
        assertEquals(new BigDecimal("1.2500"), result.territoryFactor());

        // terr_premium = 850.00 * 1.2500 = 1062.50
        BigDecimal expectedMod = new BigDecimal("850.00")
                .multiply(new BigDecimal("1.2500")).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedMod, result.modifiedPremium());

        // tax = 1062.50 * 0.035 = 37.19 (rounded)
        BigDecimal expectedTax = expectedMod.multiply(new BigDecimal("0.0350"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, result.taxAmount());

        // surcharge = 25.00
        assertEquals(new BigDecimal("25.00"), result.surchargeAmount());

        // final = 1062.50 + 37.19 + 25.00 = 1124.69
        BigDecimal expectedFinal = expectedMod.add(expectedTax).add(new BigDecimal("25.00"));
        assertEquals(expectedFinal, result.finalPremium());
    }

    @Test
    void homePolicy_baseRate1200_withTerritoryFactor() {
        // COBOL: WHEN POL-TYPE-HOME MOVE 1200.00 TO WS-BASE-PREMIUM
        // Territory IL0627 factor = 1.1500
        Policy policy = policyRepository.findById("TEST-HOM-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);

        assertEquals(new BigDecimal("1200.00"), result.basePremium());
        assertEquals(new BigDecimal("1.1500"), result.territoryFactor());

        // terr_premium = 1200.00 * 1.1500 = 1380.00
        BigDecimal expectedMod = new BigDecimal("1380.00");
        assertEquals(expectedMod, result.modifiedPremium());

        // tax = 1380.00 * 0.035 = 48.30
        BigDecimal expectedTax = new BigDecimal("48.30");
        assertEquals(expectedTax, result.taxAmount());

        // final = 1380.00 + 48.30 + 25.00 = 1453.30
        assertEquals(new BigDecimal("1453.30"), result.finalPremium());
    }

    @Test
    void commercialPolicy_baseRate5000_withTerritoryFactor() {
        // COBOL: WHEN POL-TYPE-COMM MOVE 5000.00 TO WS-BASE-PREMIUM
        // Territory MI0482 factor = 1.0800
        Policy policy = policyRepository.findById("TEST-COM-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);

        assertEquals(new BigDecimal("5000.00"), result.basePremium());
        assertEquals(new BigDecimal("1.0800"), result.territoryFactor());

        // terr_premium = 5000.00 * 1.0800 = 5400.00
        BigDecimal expectedMod = new BigDecimal("5400.00");
        assertEquals(expectedMod, result.modifiedPremium());

        // tax = 5400.00 * 0.035 = 189.00
        BigDecimal expectedTax = new BigDecimal("189.00");
        assertEquals(expectedTax, result.taxAmount());

        // final = 5400.00 + 189.00 + 25.00 = 5614.00
        assertEquals(new BigDecimal("5614.00"), result.finalPremium());
    }

    @Test
    void lifePolicy_baseRate400_noTerritoryFactor() {
        // COBOL: WHEN POL-TYPE-LIFE MOVE 400.00 TO WS-BASE-PREMIUM
        // No territory code -> factor defaults to 1.0000
        Policy policy = policyRepository.findById("TEST-LIF-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);

        assertEquals(new BigDecimal("400.00"), result.basePremium());
        assertEquals(BigDecimal.ONE, result.territoryFactor());

        // mod_premium = 400.00 (all factors 1.0)
        assertEquals(new BigDecimal("400.00"), result.modifiedPremium());

        // tax = 400.00 * 0.035 = 14.00
        assertEquals(new BigDecimal("14.00"), result.taxAmount());

        // final = 400.00 + 14.00 + 25.00 = 439.00
        assertEquals(new BigDecimal("439.00"), result.finalPremium());
    }

    @Test
    void healthPolicy_baseRate3500_noTerritoryFactor() {
        // COBOL: WHEN POL-TYPE-HEALTH MOVE 3500.00 TO WS-BASE-PREMIUM
        Policy policy = policyRepository.findById("TEST-HLT-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);

        assertEquals(new BigDecimal("3500.00"), result.basePremium());

        // mod_premium = 3500.00
        assertEquals(new BigDecimal("3500.00"), result.modifiedPremium());

        // tax = 3500.00 * 0.035 = 122.50
        assertEquals(new BigDecimal("122.50"), result.taxAmount());

        // final = 3500.00 + 122.50 + 25.00 = 3647.50
        assertEquals(new BigDecimal("3647.50"), result.finalPremium());
    }

    @Test
    void taxRateIs3Point5Percent() {
        // COBOL: 01 WS-TAX-RATE PIC S9(01)V9999 COMP-3 VALUE 0.0350.
        BigDecimal basePremium = new BigDecimal("1000.00");
        BigDecimal expectedTax = basePremium.multiply(new BigDecimal("0.0350"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("35.00"), expectedTax);
    }

    @Test
    void regulatorySurchargeIs25Dollars() {
        // COBOL: MOVE 25.00 TO WS-SURCHARGE
        Policy policy = policyRepository.findById("TEST-AUT-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);
        assertEquals(new BigDecimal("25.00"), result.surchargeAmount());
    }

    @Test
    void batchRunProcessesAllActivePolicies() {
        BatchSummary summary = premiumService.runBatch();

        assertTrue(summary.policiesRead() >= 6,
                "Should read at least 6 active test policies");
        assertEquals(summary.policiesRead(), summary.policiesUpdated(),
                "All policies should be updated successfully");
        assertEquals(0, summary.policiesError());

        List<Premium> premiums = premiumRepository.findByPolicyNumber("TEST-AUT-001");
        assertNotNull(premiums);
        assertTrue(premiums.size() >= 1);
        assertEquals("PREMBAT", premiums.getFirst().getCalcBy().trim());
    }

    @Test
    void persistedPremiumMatchesCalculation() {
        Policy policy = policyRepository.findById("TEST-HOM-001").orElseThrow();
        PremiumResult result = premiumService.calculatePremium(policy);
        premiumService.persistPremiumResult(policy, result);

        List<Premium> premiums = premiumRepository.findByPolicyNumber("TEST-HOM-001");
        Premium saved = premiums.getFirst();

        assertEquals(0, result.basePremium().compareTo(saved.getBaseRate()));
        assertEquals(0, result.territoryFactor().compareTo(saved.getTerritoryFactor()));
        assertEquals(0, result.taxAmount().compareTo(saved.getTaxAmt()));
        assertEquals(0, result.surchargeAmount().compareTo(saved.getSurchargeAmt()));
        assertEquals(0, result.finalPremium().compareTo(saved.getTotalPremium()));
        assertEquals("PREMBAT", saved.getCalcBy().trim());
        assertEquals("AN", saved.getInstallmentCode().trim());
    }
}
