package com.acme.insurance.pas.batch;

import com.acme.insurance.pas.batch.model.Policy;
import com.acme.insurance.pas.batch.model.PremiumRecord;
import com.acme.insurance.pas.batch.model.TerritoryFactor;
import com.acme.insurance.pas.batch.repository.TerritoryFactorRepository;
import com.acme.insurance.pas.batch.service.PremiumCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the premium calculation logic ported from COBOL paragraph
 * 3200-CALCULATE-PREMIUM (PREMBAT.cbl lines 149-188).
 * Verifies that the Java implementation produces the same outputs as COBOL.
 */
class PremiumCalculationServiceTest {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0350");
    private static final BigDecimal SURCHARGE = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_BASE = new BigDecimal("1000.00");

    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
            "AUT", new BigDecimal("850.00"),
            "HOM", new BigDecimal("1200.00"),
            "COM", new BigDecimal("5000.00"),
            "LIF", new BigDecimal("400.00"),
            "HLT", new BigDecimal("3500.00")
    );

    private TerritoryFactorRepository territoryFactorRepository;
    private PremiumCalculationService service;

    @BeforeEach
    void setUp() {
        territoryFactorRepository = mock(TerritoryFactorRepository.class);
        when(territoryFactorRepository.findByCodeAndDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service = new PremiumCalculationService(territoryFactorRepository);
        service.setTaxRate(TAX_RATE);
        service.setSurcharge(SURCHARGE);
        service.setBaseRates(BASE_RATES);
        service.setDefaultBaseRate(DEFAULT_BASE);
    }

    /**
     * Verify each policy type's base rate matches the COBOL EVALUATE
     * (PREMBAT.cbl lines 151-164).
     */
    @ParameterizedTest(name = "Policy type {0} should have base rate {1}")
    @CsvSource({
            "AUT, 850.00",
            "HOM, 1200.00",
            "COM, 5000.00",
            "LIF, 400.00",
            "HLT, 3500.00"
    })
    void testBaseRateByPolicyType(String policyType, String expectedBaseRate) {
        Policy policy = makePolicy("POL-XX-00001", policyType);
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal(expectedBaseRate), result.getBaseRate());
    }

    /**
     * Unknown policy type defaults to $1000 base rate
     * (COBOL WHEN OTHER, line 163).
     */
    @Test
    void testUnknownPolicyTypeDefaultsTo1000() {
        Policy policy = makePolicy("POL-XX-00001", "XYZ");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("1000.00"), result.getBaseRate());
    }

    /**
     * Verify the full premium formula for an AUTO policy matches COBOL:
     * base=850, terrFactor=1.0, classFactor=1.0, expMod=1.0,
     * tax=850*0.035=29.75, surcharge=25, total=850+29.75+25=904.75
     */
    @Test
    void testAutoFullCalculationMatchesCobol() {
        Policy policy = makePolicy("POL-AT-00001", "AUT");
        PremiumRecord result = service.calculate(policy);

        BigDecimal expectedBase = new BigDecimal("850.00");
        BigDecimal expectedTax = expectedBase.multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = expectedBase.add(expectedTax).add(SURCHARGE);

        assertEquals(expectedBase, result.getBaseRate());
        assertEquals(expectedTax, result.getTaxAmt());
        assertEquals(SURCHARGE, result.getSurchargeAmt());
        assertEquals(expectedTotal, result.getTotalPremium());
        assertEquals(BigDecimal.ONE, result.getTerritoryFactor());
        assertEquals(BigDecimal.ONE, result.getClassFactor());
        assertEquals(BigDecimal.ONE, result.getExperienceMod());
    }

    /**
     * Verify the full premium formula for a COMMERCIAL policy:
     * base=5000, tax=5000*0.035=175.00, surcharge=25, total=5200.00
     */
    @Test
    void testCommercialFullCalculation() {
        Policy policy = makePolicy("POL-CM-00001", "COM");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("5000.00"), result.getBaseRate());
        assertEquals(new BigDecimal("175.00"), result.getTaxAmt());
        assertEquals(new BigDecimal("5200.00"), result.getTotalPremium());
    }

    /**
     * When a territory factor exists in the DB, it should be applied.
     * This is the improvement over COBOL (which had 1.0 hardcoded).
     */
    @Test
    void testTerritoryFactorApplied() {
        TerritoryFactor tf = new TerritoryFactor();
        tf.setTerritoryCode("POL-HO");
        tf.setRatingFactor(new BigDecimal("1.1500"));
        when(territoryFactorRepository.findByCodeAndDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(tf));

        Policy policy = makePolicy("POL-HO-00001", "HOM");
        PremiumRecord result = service.calculate(policy);

        BigDecimal expectedBase = new BigDecimal("1200.00");
        BigDecimal expectedModPremium = expectedBase.multiply(new BigDecimal("1.1500"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTax = expectedModPremium.multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = expectedModPremium.add(expectedTax).add(SURCHARGE);

        assertEquals(new BigDecimal("1.1500"), result.getTerritoryFactor());
        assertEquals(expectedTotal, result.getTotalPremium());
    }

    /**
     * Verify record metadata fields are set correctly
     * (mirrors COBOL 3300-WRITE-PREMIUM-RECORD, lines 191-200).
     */
    @Test
    void testRecordMetadata() {
        Policy policy = makePolicy("POL-LF-00001", "LIF");
        PremiumRecord result = service.calculate(policy);

        assertEquals("POL-LF-00001", result.getPolicyNumber());
        assertEquals(1, result.getCoverageSeq());
        assertEquals(LocalDate.of(2025, 1, 1), result.getTermEffDate());
        assertEquals(LocalDate.of(2026, 1, 1), result.getTermExpDate());
        assertEquals("AN", result.getInstallmentCode());
        assertEquals("PASBATCH", result.getCalcBy());
        assertEquals(LocalDate.now(), result.getCalcDate());
        assertNotNull(result.getScheduleMod());
        assertNotNull(result.getDiscountPct());
    }

    /**
     * All five policy types should produce the same formula structure.
     */
    @ParameterizedTest(name = "Policy type {0}: total = base + base*0.035 + 25")
    @CsvSource({
            "AUT, 850.00,  904.75",
            "HOM, 1200.00, 1267.00",
            "COM, 5000.00, 5200.00",
            "LIF, 400.00,  439.00",
            "HLT, 3500.00, 3647.50"
    })
    void testTotalPremiumByType(String type, String base, String expectedTotal) {
        Policy policy = makePolicy("POL-XX-00001", type);
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal(expectedTotal), result.getTotalPremium());
    }

    private Policy makePolicy(String policyNumber, String policyType) {
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(policyType);
        policy.setPolicyStatus("AC");
        policy.setEffectiveDate(LocalDate.of(2025, 1, 1));
        policy.setExpiryDate(LocalDate.of(2026, 1, 1));
        policy.setTotalPremium(BigDecimal.ZERO);
        policy.setDeductible(new BigDecimal("500.00"));
        policy.setCoverageLimit(new BigDecimal("100000.00"));
        return policy;
    }
}
