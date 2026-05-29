package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.Premium;
import com.acme.insurance.pas.model.PremiumCalcResponse;
import com.acme.insurance.pas.model.PremiumCalcResult;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Premium Batch Calculation Service.
 * Migrated from COBOL program PREMBAT (cobol/programs/PREMBAT.cbl).
 *
 * Mirrors the COBOL paragraph flow:
 *   1000-INITIALIZE        -> calculatePremiums() entry
 *   2000-LOAD-RATING-TABLES -> territory/class/experience factors (defaults to 1.0)
 *   3000-PROCESS-POLICIES   -> iterate active policies
 *   3100-FETCH-POLICY       -> fetched via findAllActivePolicies()
 *   3200-CALCULATE-PREMIUM  -> calculateBasePremium() + tax + surcharge
 *   3300-WRITE-PREMIUM-RECORD -> insertPremium()
 *   4000-WRITE-SUMMARY      -> PremiumCalcResponse
 *   9999-TERMINATE          -> return response
 */
@Service
public class PremiumBatchService {

    private static final Logger logger = LoggerFactory.getLogger(PremiumBatchService.class);

    private static final BigDecimal BASE_RATE_AUTO = new BigDecimal("850.00");
    private static final BigDecimal BASE_RATE_HOME = new BigDecimal("1200.00");
    private static final BigDecimal BASE_RATE_COMMERCIAL = new BigDecimal("5000.00");
    private static final BigDecimal BASE_RATE_LIFE = new BigDecimal("400.00");
    private static final BigDecimal BASE_RATE_HEALTH = new BigDecimal("3500.00");
    private static final BigDecimal BASE_RATE_OTHER = new BigDecimal("1000.00");

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0350");
    private static final BigDecimal SURCHARGE = new BigDecimal("25.00");

    private static final BigDecimal DEFAULT_FACTOR = new BigDecimal("1.0000");
    private static final BigDecimal DEFAULT_DISCOUNT = BigDecimal.ZERO;

    @Autowired
    private PolicyRepository policyRepository;

    public PremiumCalcResponse calculatePremiums() {
        logger.info("PREMBAT: Starting premium batch calculation");
        Date calcDate = new Date();

        int policiesRead = 0;
        int policiesUpdated = 0;
        int policiesError = 0;
        List<PremiumCalcResult> results = new ArrayList<PremiumCalcResult>();

        List<Policy> activePolicies = policyRepository.findAllActivePolicies();

        for (Policy policy : activePolicies) {
            policiesRead++;
            PremiumCalcResult result = new PremiumCalcResult();
            result.setPolicyNumber(policy.getPolicyNumber());
            result.setPolicyType(policy.getPolicyType());

            try {
                BigDecimal baseRate = getBaseRate(policy.getPolicyType());

                BigDecimal terrPremium = baseRate.multiply(DEFAULT_FACTOR);
                BigDecimal classPremium = terrPremium.multiply(DEFAULT_FACTOR);
                BigDecimal modPremium = classPremium.multiply(DEFAULT_FACTOR);

                BigDecimal taxAmount = modPremium.multiply(TAX_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal finalPremium = modPremium.add(taxAmount).add(SURCHARGE)
                        .setScale(2, RoundingMode.HALF_UP);

                Premium premium = new Premium();
                premium.setPolicyNumber(policy.getPolicyNumber());
                premium.setCoverageSeq(1);
                premium.setTermEffectiveDate(policy.getEffectiveDate());
                premium.setTermExpiryDate(policy.getExpiryDate());
                premium.setBaseRate(baseRate);
                premium.setTerritoryFactor(DEFAULT_FACTOR);
                premium.setClassFactor(DEFAULT_FACTOR);
                premium.setExperienceMod(DEFAULT_FACTOR);
                premium.setScheduleMod(DEFAULT_FACTOR);
                premium.setDiscountPct(DEFAULT_DISCOUNT);
                premium.setSurchargeAmt(SURCHARGE);
                premium.setTaxAmt(taxAmount);
                premium.setTotalPremium(finalPremium);
                premium.setInstallmentCode("AN");
                premium.setCalcDate(calcDate);
                premium.setCalcBy("PREMBAT");

                policyRepository.insertPremium(premium);
                policiesUpdated++;

                result.setBasePremium(baseRate);
                result.setTaxAmount(taxAmount);
                result.setSurchargeAmount(SURCHARGE);
                result.setTotalPremium(finalPremium);
                result.setStatus("SUCCESS");
            } catch (Exception e) {
                policiesError++;
                result.setStatus("ERROR");
                logger.error("PREMBAT: Error processing policy {}: {}",
                        policy.getPolicyNumber(), e.getMessage());
            }

            results.add(result);
        }

        logger.info("PREMBAT: Batch complete - read={}, updated={}, errors={}",
                policiesRead, policiesUpdated, policiesError);

        PremiumCalcResponse response = new PremiumCalcResponse();
        response.setPoliciesRead(policiesRead);
        response.setPoliciesUpdated(policiesUpdated);
        response.setPoliciesError(policiesError);
        response.setResults(results);
        return response;
    }

    private BigDecimal getBaseRate(String policyType) {
        if (policyType == null) {
            return BASE_RATE_OTHER;
        }
        switch (policyType.trim()) {
            case "AUT": return BASE_RATE_AUTO;
            case "HOM": return BASE_RATE_HOME;
            case "COM": return BASE_RATE_COMMERCIAL;
            case "LIF": return BASE_RATE_LIFE;
            case "HLT": return BASE_RATE_HEALTH;
            default:    return BASE_RATE_OTHER;
        }
    }
}
