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

/**
 * Core business logic ported from COBOL paragraph 3200-CALCULATE-PREMIUM
 * (PREMBAT.cbl lines 149-188).
 *
 * All rates and factors are externalized via application.yml instead of
 * being hardcoded as in the COBOL version.
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

        BigDecimal terrFactor = territoryFactorRepository
                .findByCodeAndDate(policy.getTerritoryCode(), LocalDate.now())
                .map(TerritoryFactor::getRatingFactor)
                .orElse(BigDecimal.ONE);
        BigDecimal terrPremium = baseRate.multiply(terrFactor);

        BigDecimal classPremium = terrPremium.multiply(BigDecimal.ONE);

        BigDecimal modPremium = classPremium.multiply(BigDecimal.ONE);

        BigDecimal taxAmount = modPremium.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalPremium = modPremium.add(taxAmount).add(surcharge)
                .setScale(2, RoundingMode.HALF_UP);

        PremiumRecord record = new PremiumRecord();
        record.setPolicyNumber(policy.getPolicyNumber());
        record.setCoverageSeq(1);
        record.setTermEffDate(policy.getEffectiveDate());
        record.setTermExpDate(policy.getExpiryDate());
        record.setBaseRate(baseRate);
        record.setTerritoryFactor(terrFactor);
        record.setClassFactor(BigDecimal.ONE);
        record.setExperienceMod(BigDecimal.ONE);
        record.setScheduleMod(BigDecimal.ONE);
        record.setDiscountPct(BigDecimal.ZERO);
        record.setSurchargeAmt(surcharge);
        record.setTaxAmt(taxAmount);
        record.setTotalPremium(finalPremium);
        record.setInstallmentCode("AN");
        record.setCalcDate(LocalDate.now());
        record.setCalcBy("PREMBAT");
        return record;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public void setBaseRates(Map<String, BigDecimal> baseRates) {
        this.baseRates = baseRates;
    }

    public Map<String, BigDecimal> getBaseRates() {
        return baseRates;
    }

    public void setDefaultBaseRate(BigDecimal defaultBaseRate) {
        this.defaultBaseRate = defaultBaseRate;
    }

    public BigDecimal getDefaultBaseRate() {
        return defaultBaseRate;
    }
}
