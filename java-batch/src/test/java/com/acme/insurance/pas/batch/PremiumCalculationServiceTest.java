package com.acme.insurance.pas.batch;

import com.acme.insurance.pas.batch.model.Policy;
import com.acme.insurance.pas.batch.model.PremiumRecord;
import com.acme.insurance.pas.batch.model.TerritoryFactor;
import com.acme.insurance.pas.batch.repository.TerritoryFactorRepository;
import com.acme.insurance.pas.batch.service.PremiumCalculationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PremiumCalculationServiceTest {

    private PremiumCalculationService service;
    private TerritoryFactorRepository territoryFactorRepository;

    @BeforeEach
    void setUp() {
        territoryFactorRepository = mock(TerritoryFactorRepository.class);
        when(territoryFactorRepository.findByCodeAndDate(any(), any()))
                .thenReturn(Optional.empty());

        service = new PremiumCalculationService(territoryFactorRepository);
        service.setTaxRate(new BigDecimal("0.0350"));
        service.setSurcharge(new BigDecimal("25.00"));

        Map<String, BigDecimal> baseRates = new HashMap<>();
        baseRates.put("AUT", new BigDecimal("850.00"));
        baseRates.put("HOM", new BigDecimal("1200.00"));
        baseRates.put("COM", new BigDecimal("5000.00"));
        baseRates.put("LIF", new BigDecimal("400.00"));
        baseRates.put("HLT", new BigDecimal("3500.00"));
        service.setBaseRates(baseRates);
    }

    @Test
    void calculateAutoPolicy() {
        Policy policy = createPolicy("POL-TEST-0001", "AUT");
        PremiumRecord result = service.calculate(policy);

        assertNotNull(result);
        assertEquals(new BigDecimal("850.00"), result.getBaseRate());
        // tax = 850.00 * 0.0350 = 29.75
        assertEquals(new BigDecimal("29.75"), result.getTaxAmt());
        // final = 850.00 + 29.75 + 25.00 = 904.75
        assertEquals(new BigDecimal("904.75"), result.getTotalPremium());
        assertEquals("AN", result.getInstallmentCode());
        assertEquals("PREMBAT", result.getCalcBy());
    }

    @Test
    void calculateHomePolicy() {
        Policy policy = createPolicy("POL-TEST-0002", "HOM");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("1200.00"), result.getBaseRate());
        // tax = 1200.00 * 0.0350 = 42.00
        assertEquals(new BigDecimal("42.00"), result.getTaxAmt());
        // final = 1200.00 + 42.00 + 25.00 = 1267.00
        assertEquals(new BigDecimal("1267.00"), result.getTotalPremium());
    }

    @Test
    void calculateCommercialPolicy() {
        Policy policy = createPolicy("POL-TEST-0003", "COM");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("5000.00"), result.getBaseRate());
        // tax = 5000.00 * 0.0350 = 175.00
        assertEquals(new BigDecimal("175.00"), result.getTaxAmt());
        // final = 5000.00 + 175.00 + 25.00 = 5200.00
        assertEquals(new BigDecimal("5200.00"), result.getTotalPremium());
    }

    @Test
    void calculateLifePolicy() {
        Policy policy = createPolicy("POL-TEST-0004", "LIF");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("400.00"), result.getBaseRate());
        // tax = 400.00 * 0.0350 = 14.00
        assertEquals(new BigDecimal("14.00"), result.getTaxAmt());
        // final = 400.00 + 14.00 + 25.00 = 439.00
        assertEquals(new BigDecimal("439.00"), result.getTotalPremium());
    }

    @Test
    void calculateHealthPolicy() {
        Policy policy = createPolicy("POL-TEST-0005", "HLT");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("3500.00"), result.getBaseRate());
        // tax = 3500.00 * 0.0350 = 122.50
        assertEquals(new BigDecimal("122.50"), result.getTaxAmt());
        // final = 3500.00 + 122.50 + 25.00 = 3647.50
        assertEquals(new BigDecimal("3647.50"), result.getTotalPremium());
    }

    @Test
    void calculateUnknownPolicyTypeDefaultsTo1000() {
        Policy policy = createPolicy("POL-TEST-0006", "XYZ");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("1000.00"), result.getBaseRate());
        // tax = 1000.00 * 0.0350 = 35.00
        assertEquals(new BigDecimal("35.00"), result.getTaxAmt());
        // final = 1000.00 + 35.00 + 25.00 = 1060.00
        assertEquals(new BigDecimal("1060.00"), result.getTotalPremium());
    }

    @Test
    void calculateWithTerritoryFactor() {
        TerritoryFactor tf = new TerritoryFactor();
        tf.setTerritoryCode("IL0627");
        tf.setEffectiveDate(LocalDate.of(2024, 1, 1));
        tf.setRatingFactor(new BigDecimal("1.1500"));

        when(territoryFactorRepository.findByCodeAndDate(eq("IL0627"), any()))
                .thenReturn(Optional.of(tf));

        Policy policy = createPolicy("POL-TEST-0007", "HOM");
        policy.setTerritoryCode("IL0627");
        PremiumRecord result = service.calculate(policy);

        assertEquals(new BigDecimal("1200.00"), result.getBaseRate());
        assertEquals(new BigDecimal("1.1500"), result.getTerritoryFactor());
        // terrPremium = 1200.00 * 1.15 = 1380.0000
        // tax = 1380.00 * 0.0350 = 48.30
        assertEquals(new BigDecimal("48.30"), result.getTaxAmt());
        // final = 1380.00 + 48.30 + 25.00 = 1453.30
        assertEquals(new BigDecimal("1453.30"), result.getTotalPremium());
    }

    @Test
    void policyFieldsMappedCorrectly() {
        Policy policy = createPolicy("POL-TEST-0008", "AUT");
        PremiumRecord result = service.calculate(policy);

        assertEquals("POL-TEST-0008", result.getPolicyNumber());
        assertEquals(1, result.getCoverageSeq());
        assertEquals(LocalDate.of(2025, 1, 1), result.getTermEffDate());
        assertEquals(LocalDate.of(2026, 1, 1), result.getTermExpDate());
        assertEquals(BigDecimal.ONE, result.getClassFactor());
        assertEquals(BigDecimal.ONE, result.getExperienceMod());
        assertEquals(BigDecimal.ONE, result.getScheduleMod());
        assertEquals(BigDecimal.ZERO, result.getDiscountPct());
        assertEquals(new BigDecimal("25.00"), result.getSurchargeAmt());
        assertEquals(LocalDate.now(), result.getCalcDate());
    }

    private Policy createPolicy(String policyNumber, String policyType) {
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(policyType);
        policy.setPolicyStatus("AC");
        policy.setEffectiveDate(LocalDate.of(2025, 1, 1));
        policy.setExpiryDate(LocalDate.of(2026, 1, 1));
        policy.setTotalPremium(BigDecimal.ZERO);
        policy.setDeductible(BigDecimal.ZERO);
        policy.setCoverageLimit(BigDecimal.ZERO);
        return policy;
    }
}
