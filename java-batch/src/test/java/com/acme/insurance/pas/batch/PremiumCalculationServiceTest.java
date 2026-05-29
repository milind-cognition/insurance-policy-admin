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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PremiumCalculationService verifying parity with
 * COBOL 3200-CALCULATE-PREMIUM logic (PREMBAT.cbl lines 149-188).
 */
class PremiumCalculationServiceTest {

    private PremiumCalculationService service;
    private TerritoryFactorRepository territoryFactorRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0350");
    private static final BigDecimal SURCHARGE = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("1000.00");

    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
            "AUT", new BigDecimal("850.00"),
            "HOM", new BigDecimal("1200.00"),
            "COM", new BigDecimal("5000.00"),
            "LIF", new BigDecimal("400.00"),
            "HLT", new BigDecimal("3500.00")
    );

    @BeforeEach
    void setUp() {
        territoryFactorRepository = mock(TerritoryFactorRepository.class);
        when(territoryFactorRepository.findByCodeAndDate(anyString(), any()))
                .thenReturn(Optional.empty());

        service = new PremiumCalculationService(territoryFactorRepository);
        service.setTaxRate(TAX_RATE);
        service.setSurcharge(SURCHARGE);
        service.setBaseRates(BASE_RATES);
        service.setDefaultBaseRate(DEFAULT_BASE_RATE);
    }

    @ParameterizedTest(name = "Policy type {0} -> base rate {1}")
    @CsvSource({
            "AUT, 850.00",
            "HOM, 1200.00",
            "COM, 5000.00",
            "LIF, 400.00",
            "HLT, 3500.00"
    })
    void shouldCalculateCorrectBaseRateByPolicyType(String policyType, String expectedBaseRate) {
        Policy policy = buildPolicy(policyType);
        PremiumRecord result = service.calculate(policy);
        assertEquals(new BigDecimal(expectedBaseRate), result.getBaseRate());
    }

    @Test
    void shouldUseDefaultBaseRateForUnknownPolicyType() {
        Policy policy = buildPolicy("XYZ");
        PremiumRecord result = service.calculate(policy);
        assertEquals(DEFAULT_BASE_RATE, result.getBaseRate());
    }

    @ParameterizedTest(name = "Policy type {0} -> final premium {1}")
    @CsvSource({
            "AUT, 904.75",
            "HOM, 1267.00",
            "COM, 5200.00",
            "LIF, 439.00",
            "HLT, 3647.50"
    })
    void shouldCalculateCorrectFinalPremium(String policyType, String expectedFinal) {
        Policy policy = buildPolicy(policyType);
        PremiumRecord result = service.calculate(policy);
        assertEquals(new BigDecimal(expectedFinal), result.getTotalPremium());
    }

    @Test
    void shouldCalculateFinalPremiumForUnknownType() {
        // base = 1000, tax = 1000 * 0.035 = 35.00, surcharge = 25.00
        // final = 1000 + 35 + 25 = 1060.00
        Policy policy = buildPolicy("XYZ");
        PremiumRecord result = service.calculate(policy);
        assertEquals(new BigDecimal("1060.00"), result.getTotalPremium());
    }

    @Test
    void shouldCalculateCorrectTaxAmount() {
        Policy policy = buildPolicy("AUT");
        PremiumRecord result = service.calculate(policy);
        BigDecimal expectedTax = new BigDecimal("850.00").multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, result.getTaxAmt());
    }

    @Test
    void shouldApplyFlatSurcharge() {
        Policy policy = buildPolicy("AUT");
        PremiumRecord result = service.calculate(policy);
        assertEquals(SURCHARGE, result.getSurchargeAmt());
    }

    @Test
    void shouldApplyTerritoryFactor() {
        TerritoryFactor tf = new TerritoryFactor();
        tf.setTerritoryCode("NY0001");
        tf.setRatingFactor(new BigDecimal("1.2500"));
        tf.setEffectiveDate(LocalDate.of(2024, 1, 1));

        when(territoryFactorRepository.findByCodeAndDate(anyString(), any()))
                .thenReturn(Optional.of(tf));

        Policy policy = buildPolicy("AUT");
        policy.setTerritoryCode("NY0001");

        PremiumRecord result = service.calculate(policy);

        // base=850, terr=850*1.25=1062.50, tax=1062.50*0.035=37.19, final=1062.50+37.19+25=1124.69
        assertEquals(new BigDecimal("1.2500"), result.getTerritoryFactor());
        assertEquals(new BigDecimal("1124.69"), result.getTotalPremium());
    }

    @Test
    void shouldSetDefaultFactorsToOne() {
        Policy policy = buildPolicy("AUT");
        PremiumRecord result = service.calculate(policy);

        assertEquals(BigDecimal.ONE, result.getClassFactor());
        assertEquals(BigDecimal.ONE, result.getExperienceMod());
        assertEquals(BigDecimal.ONE, result.getScheduleMod());
        assertEquals(BigDecimal.ZERO, result.getDiscountPct());
    }

    @Test
    void shouldSetMetadataFieldsCorrectly() {
        Policy policy = buildPolicy("HOM");
        PremiumRecord result = service.calculate(policy);

        assertEquals("POL-000000001", result.getPolicyNumber());
        assertEquals(1, result.getCoverageSeq());
        assertEquals("AN", result.getInstallmentCode());
        assertEquals(LocalDate.now(), result.getCalcDate());
        assertEquals("PREMBAT", result.getCalcBy());
        assertEquals(LocalDate.of(2024, 1, 1), result.getTermEffDate());
        assertEquals(LocalDate.of(2025, 1, 1), result.getTermExpDate());
    }

    private Policy buildPolicy(String policyType) {
        Policy p = new Policy();
        p.setPolicyNumber("POL-000000001");
        p.setPolicyType(policyType);
        p.setPolicyStatus("AC");
        p.setEffectiveDate(LocalDate.of(2024, 1, 1));
        p.setExpiryDate(LocalDate.of(2025, 1, 1));
        p.setTotalPremium(BigDecimal.ZERO);
        p.setDeductible(new BigDecimal("500.00"));
        p.setCoverageLimit(new BigDecimal("100000.00"));
        return p;
    }
}
