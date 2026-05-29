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

@Service
@ConfigurationProperties(prefix = "premium")
public class PremiumCalculationService {

    private BigDecimal taxRate;
    private BigDecimal surcharge;
    private Map<String, BigDecimal> baseRates;

    private final TerritoryFactorRepository territoryFactorRepository;

    public PremiumCalculationService(TerritoryFactorRepository territoryFactorRepository) {
        this.territoryFactorRepository = territoryFactorRepository;
    }

    public PremiumRecord calculate(Policy policy) {
        BigDecimal baseRate = baseRates.getOrDefault(
                policy.getPolicyType(), new BigDecimal("1000.00"));

        BigDecimal terrFactor = BigDecimal.ONE;
        if (policy.getTerritoryCode() != null) {
            terrFactor = territoryFactorRepository
                    .findByCodeAndDate(policy.getTerritoryCode(), LocalDate.now())
                    .map(TerritoryFactor::getRatingFactor)
                    .orElse(BigDecimal.ONE);
        }
        BigDecimal terrPremium = baseRate.multiply(terrFactor)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal classFactor = BigDecimal.ONE;
        BigDecimal classPremium = terrPremium.multiply(classFactor)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal experienceMod = BigDecimal.ONE;
        BigDecimal modPremium = classPremium.multiply(experienceMod)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal taxAmount = modPremium.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalPremium = modPremium.setScale(2, RoundingMode.HALF_UP)
                .add(taxAmount)
                .add(surcharge);

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
        record.setCalcBy("PREMBAT");
        return record;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public void setBaseRates(Map<String, BigDecimal> baseRates) {
        this.baseRates = baseRates;
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
}
