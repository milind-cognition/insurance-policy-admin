package com.acme.insurance.pas.batch.service;

import com.acme.insurance.pas.batch.model.Policy;
import com.acme.insurance.pas.batch.model.PremiumRecord;
import com.acme.insurance.pas.batch.model.TerritoryFactor;
import com.acme.insurance.pas.batch.repository.TerritoryFactorRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Core premium calculation logic ported from COBOL paragraph
 * 3200-CALCULATE-PREMIUM (PREMBAT.cbl lines 149-188).
 *
 * <p>Improvements over COBOL version:
 * <ul>
 *   <li>Base rates, tax rate, and surcharge are externalized to application.yml</li>
 *   <li>Territory factors are actually loaded from the DB (COBOL had this stubbed)</li>
 *   <li>Uses java.time.LocalDate instead of integer date arithmetic</li>
 * </ul>
 */
@Service
@ConfigurationProperties(prefix = "premium")
public class PremiumCalculationService {

    private BigDecimal taxRate;
    private BigDecimal surcharge;
    private Map<String, BigDecimal> baseRates;
    private BigDecimal defaultBaseRate;

    private final TerritoryFactorRepository territoryFactorRepository;

    public PremiumCalculationService(TerritoryFactorRepository territoryFactorRepository) {
        this.territoryFactorRepository = territoryFactorRepository;
    }

    public PremiumRecord calculate(Policy policy) {
        BigDecimal baseRate = baseRates.getOrDefault(
                policy.getPolicyType(), defaultBaseRate);

        // Territory factor — loaded from TERRITORY_FACTORS table using COVERAGES.RATING_TERRITORY
        String territoryCode = policy.getRatingTerritory();
        Optional<TerritoryFactor> terrFactorOpt = territoryCode != null && !territoryCode.isBlank()
                ? territoryFactorRepository.findByCodeAndDate(territoryCode, LocalDate.now())
                : Optional.empty();
        BigDecimal terrFactor = terrFactorOpt
                .map(TerritoryFactor::getRatingFactor)
                .orElse(BigDecimal.ONE);
        BigDecimal terrPremium = baseRate.multiply(terrFactor)
                .setScale(4, RoundingMode.HALF_UP);

        // Class factor (default 1.0 — same as COBOL line 171-172)
        BigDecimal classFactor = BigDecimal.ONE;
        BigDecimal classPremium = terrPremium.multiply(classFactor)
                .setScale(4, RoundingMode.HALF_UP);

        // Experience modification (default 1.0 — same as COBOL line 175-176)
        BigDecimal experienceMod = BigDecimal.ONE;
        BigDecimal modPremium = classPremium.multiply(experienceMod)
                .setScale(2, RoundingMode.HALF_UP);

        // Tax (configurable via application.yml — COBOL had 3.5% hardcoded)
        BigDecimal taxAmount = modPremium.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Final premium = modified premium + tax + surcharge
        BigDecimal finalPremium = modPremium.add(taxAmount).add(surcharge);

        PremiumRecord record = new PremiumRecord();
        record.setPolicyNumber(policy.getPolicyNumber());
        record.setCoverageSeq(1);
        record.setTermEffDate(policy.getEffectiveDate());
        record.setTermExpDate(policy.getExpiryDate());
        record.setBaseRate(baseRate);
        record.setTerritoryFactor(terrFactor);
        record.setClassFactor(classFactor);
        record.setExperienceMod(experienceMod);
        record.setScheduleMod(BigDecimal.ONE);
        record.setDiscountPct(BigDecimal.ZERO);
        record.setSurchargeAmt(surcharge);
        record.setTaxAmt(taxAmount);
        record.setTotalPremium(finalPremium);
        record.setInstallmentCode("AN");
        record.setCalcDate(LocalDate.now());
        record.setCalcBy("PASBATCH");
        return record;
    }

    // --- Configuration property setters (bound from application.yml) ---

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public void setBaseRates(Map<String, BigDecimal> baseRates) {
        this.baseRates = baseRates;
    }

    public void setDefaultBaseRate(BigDecimal defaultBaseRate) {
        this.defaultBaseRate = defaultBaseRate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public Map<String, BigDecimal> getBaseRates() {
        return baseRates;
    }

    public BigDecimal getDefaultBaseRate() {
        return defaultBaseRate;
    }
}
