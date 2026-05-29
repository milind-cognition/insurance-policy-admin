package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.PremiumBatchSummary;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.Premium;
import com.acme.insurance.pas.model.TerritoryFactor;
import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.PremiumRepository;
import com.acme.insurance.pas.repository.TerritoryFactorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PremiumCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PremiumCalculationService.class);

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0350");
    private static final BigDecimal SURCHARGE = new BigDecimal("25.00");

    private static final Map<String, BigDecimal> BASE_RATES = new HashMap<>();
    static {
        BASE_RATES.put(Policy.TYPE_AUTO, new BigDecimal("850.00"));
        BASE_RATES.put(Policy.TYPE_HOME, new BigDecimal("1200.00"));
        BASE_RATES.put(Policy.TYPE_COMMERCIAL, new BigDecimal("5000.00"));
        BASE_RATES.put(Policy.TYPE_LIFE, new BigDecimal("400.00"));
        BASE_RATES.put(Policy.TYPE_HEALTH, new BigDecimal("3500.00"));
    }
    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("1000.00");

    private final PolicyRepository policyRepository;
    private final PremiumRepository premiumRepository;
    private final TerritoryFactorRepository territoryFactorRepository;
    private final CoverageRepository coverageRepository;

    public PremiumCalculationService(PolicyRepository policyRepository,
                                     PremiumRepository premiumRepository,
                                     TerritoryFactorRepository territoryFactorRepository,
                                     CoverageRepository coverageRepository) {
        this.policyRepository = policyRepository;
        this.premiumRepository = premiumRepository;
        this.territoryFactorRepository = territoryFactorRepository;
        this.coverageRepository = coverageRepository;
    }

    @Transactional
    public PremiumBatchSummary calculateAllPremiums() {
        LocalDate today = LocalDate.now();

        Map<String, BigDecimal> territoryFactors = loadTerritoryFactors(today);

        List<Policy> activePolicies = policyRepository.findByPolicyStatus(Policy.STATUS_ACTIVE);

        int read = 0;
        int updated = 0;
        int errors = 0;

        for (Policy policy : activePolicies) {
            read++;
            try {
                Premium premium = calculatePremium(policy, territoryFactors, today);
                premiumRepository.save(premium);
                updated++;
            } catch (Exception e) {
                errors++;
                log.error("Error calculating premium for {}: {}",
                        policy.getPolicyNumber(), e.getMessage());
            }
        }

        return new PremiumBatchSummary(read, updated, errors, today);
    }

    private Premium calculatePremium(Policy policy,
                                     Map<String, BigDecimal> territoryFactors,
                                     LocalDate calcDate) {
        BigDecimal baseRate = BASE_RATES.getOrDefault(policy.getPolicyType(), DEFAULT_BASE_RATE);

        List<Coverage> coverages = coverageRepository
                .findByPolicyNumberOrderBySequenceNum(policy.getPolicyNumber());
        BigDecimal territoryFactor = BigDecimal.ONE;
        for (Coverage cov : coverages) {
            if (cov.getRatingTerritory() != null && !cov.getRatingTerritory().isBlank()) {
                territoryFactor = territoryFactors.getOrDefault(
                        cov.getRatingTerritory().trim(), BigDecimal.ONE);
                break;
            }
        }
        BigDecimal classFactor = BigDecimal.ONE;
        BigDecimal experienceMod = BigDecimal.ONE;

        BigDecimal modifiedPremium = baseRate
                .multiply(territoryFactor)
                .multiply(classFactor)
                .multiply(experienceMod)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = modifiedPremium.multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalPremium = modifiedPremium.add(taxAmount).add(SURCHARGE);

        Premium premium = new Premium();
        premium.setPolicyNumber(policy.getPolicyNumber());
        premium.setCoverageSeq(1);
        premium.setTermEffectiveDate(policy.getEffectiveDate());
        premium.setTermExpiryDate(policy.getExpiryDate());
        premium.setBaseRate(baseRate);
        premium.setTerritoryFactor(territoryFactor);
        premium.setClassFactor(classFactor);
        premium.setExperienceMod(experienceMod);
        premium.setScheduleMod(BigDecimal.ONE);
        premium.setDiscountPct(BigDecimal.ZERO);
        premium.setSurchargeAmt(SURCHARGE);
        premium.setTaxAmt(taxAmount);
        premium.setTotalPremium(finalPremium);
        premium.setInstallmentCode("AN");
        premium.setCalcDate(calcDate);
        premium.setCalcBy("PREMBAT");

        return premium;
    }

    private Map<String, BigDecimal> loadTerritoryFactors(LocalDate asOfDate) {
        Map<String, BigDecimal> factors = new HashMap<>();
        List<TerritoryFactor> tfList = territoryFactorRepository
                .findByEffectiveDateLessThanEqual(asOfDate);
        for (TerritoryFactor tf : tfList) {
            factors.put(tf.getTerritoryCode(), tf.getRatingFactor());
        }
        return factors;
    }
}
