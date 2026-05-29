package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.Premium;
import com.acme.insurance.pas.model.PremiumBatchResponse;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Premium Batch Calculation Service.
 * Migrated from COBOL program PREMBAT (Premium Calculation Batch).
 *
 * Replicates the COBOL paragraph flow:
 *   1000-INITIALIZE      → initialize counters, get current date
 *   2000-LOAD-RATING-TBL → (skipped — uses 1.0 defaults)
 *   3000-PROCESS-POLICIES → fetch active policies, loop
 *   3100-FETCH-POLICY     → increment read counter
 *   3200-CALCULATE-PREMIUM→ base rate by type, factors, tax, surcharge
 *   3300-WRITE-PREMIUM    → insert premium record
 *   4000-WRITE-SUMMARY    → return batch response with counts
 */
@Service
public class PremiumBatchService {

    private static final Logger log = LoggerFactory.getLogger(PremiumBatchService.class);

    private static final BigDecimal BASE_RATE_AUT = new BigDecimal("850.00");
    private static final BigDecimal BASE_RATE_HOM = new BigDecimal("1200.00");
    private static final BigDecimal BASE_RATE_COM = new BigDecimal("5000.00");
    private static final BigDecimal BASE_RATE_LIF = new BigDecimal("400.00");
    private static final BigDecimal BASE_RATE_HLT = new BigDecimal("3500.00");
    private static final BigDecimal BASE_RATE_OTHER = new BigDecimal("1000.00");

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0350");
    private static final BigDecimal SURCHARGE = new BigDecimal("25.00");
    private static final BigDecimal FACTOR_DEFAULT = new BigDecimal("1.0000");
    private static final BigDecimal DISCOUNT_DEFAULT = BigDecimal.ZERO;
    private static final int COVERAGE_SEQ = 1;
    private static final String INSTALLMENT_CODE = "AN";
    private static final String CALC_BY = "PREMBAT";

    @Autowired
    private PolicyRepository policyRepository;

    public PremiumBatchResponse calculatePremiums() {
        int policiesRead = 0;
        int policiesUpdated = 0;
        int policiesError = 0;
        Date calcDate = new Date();

        List<Policy> activePolicies = policyRepository.findActivePolicies();

        for (Policy policy : activePolicies) {
            policiesRead++;

            BigDecimal baseRate = getBaseRate(policy.getPolicyType());
            BigDecimal taxAmt = baseRate.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPremium = baseRate.add(taxAmt).add(SURCHARGE);

            Premium premium = new Premium();
            premium.setPolicyNumber(policy.getPolicyNumber());
            premium.setCoverageSeq(COVERAGE_SEQ);
            premium.setTermEffectiveDate(policy.getEffectiveDate());
            premium.setTermExpiryDate(policy.getExpiryDate());
            premium.setBaseRate(baseRate);
            premium.setTerritoryFactor(FACTOR_DEFAULT);
            premium.setClassFactor(FACTOR_DEFAULT);
            premium.setExperienceMod(FACTOR_DEFAULT);
            premium.setScheduleMod(FACTOR_DEFAULT);
            premium.setDiscountPct(DISCOUNT_DEFAULT);
            premium.setSurchargeAmt(SURCHARGE);
            premium.setTaxAmt(taxAmt);
            premium.setTotalPremium(totalPremium);
            premium.setInstallmentCode(INSTALLMENT_CODE);
            premium.setInstallmentAmt(totalPremium);
            premium.setCalcDate(calcDate);
            premium.setCalcBy(CALC_BY);

            try {
                policyRepository.insertPremium(premium);
                policiesUpdated++;
            } catch (DataAccessException e) {
                log.error("Error inserting premium for policy {}: {}",
                        policy.getPolicyNumber(), e.getMessage());
                policiesError++;
            }
        }

        PremiumBatchResponse response = new PremiumBatchResponse();
        response.setPoliciesRead(policiesRead);
        response.setPoliciesUpdated(policiesUpdated);
        response.setPoliciesError(policiesError);
        if (policiesError > 0) {
            response.setMessage("PREMBAT completed with " + policiesError + " error(s)");
        } else {
            response.setMessage("PREMBAT completed successfully");
        }

        log.info("PREMBAT summary: read={}, updated={}, errors={}",
                policiesRead, policiesUpdated, policiesError);
        return response;
    }

    private BigDecimal getBaseRate(String policyType) {
        if (policyType == null) {
            return BASE_RATE_OTHER;
        }
        switch (policyType.trim()) {
            case "AUT": return BASE_RATE_AUT;
            case "HOM": return BASE_RATE_HOM;
            case "COM":
            case "CGL": return BASE_RATE_COM;
            case "LIF": return BASE_RATE_LIF;
            case "HLT": return BASE_RATE_HLT;
            default:    return BASE_RATE_OTHER;
        }
    }
}
