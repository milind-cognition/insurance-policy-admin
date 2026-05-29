package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.RenewalResponse;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

/**
 * Renewal Service - Migrated from COBOL program POLRNW.
 * CICS Transaction: PRWL
 *
 * Mirrors the COBOL paragraph flow exactly:
 *   1000-INITIALIZE           -> (handled at method entry)
 *   2000-READ-EXISTING-POLICY -> findByPolicyNumber
 *   3000-CHECK-RENEWAL-ELIGIBILITY -> checkRenewalEligibility
 *   4000-CALCULATE-NEW-PREMIUM -> calculateNewPremium
 *   5000-APPLY-RATE-CAP       -> applyRateCap
 *   6000-CREATE-RENEWAL-TERM  -> createRenewalTerm
 *   7000-UPDATE-COVERAGES     -> updateCoverageDates
 *   8000-SEND-CONFIRMATION    -> return RenewalResponse
 */
@Service
public class RenewalService {

    private static final BigDecimal RATE_INCREASE_FACTOR = new BigDecimal("1.05");
    private static final BigDecimal RATE_INCREASE_CAP = new BigDecimal("15.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Autowired
    private PolicyRepository policyRepository;

    /**
     * Process a policy renewal mirroring COBOL POLRNW paragraph flow.
     *
     * @param policyNumber the policy to renew
     * @return RenewalResponse on success, null if policy not found
     * @throws IllegalArgumentException if policy is not eligible for renewal
     */
    @Transactional
    public RenewalResponse renewPolicy(String policyNumber) {
        // 2000-READ-EXISTING-POLICY
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null;
        }

        // 3000-CHECK-RENEWAL-ELIGIBILITY
        checkRenewalEligibility(policy);

        BigDecimal oldPremium = policy.getTotalPremium();
        if (oldPremium == null || oldPremium.signum() == 0) {
            throw new IllegalArgumentException("POLICY PREMIUM IS ZERO OR NULL");
        }

        // 4000-CALCULATE-NEW-PREMIUM
        BigDecimal newPremium = calculateNewPremium(oldPremium);

        // 5000-APPLY-RATE-CAP
        BigDecimal rateChangePct = newPremium.subtract(oldPremium)
                .multiply(ONE_HUNDRED)
                .divide(oldPremium, 2, RoundingMode.DOWN);
        boolean rateCapped = false;
        if (rateChangePct.compareTo(RATE_INCREASE_CAP) > 0) {
            newPremium = applyRateCap(oldPremium);
            rateChangePct = RATE_INCREASE_CAP;
            rateCapped = true;
        }

        // 6000-CREATE-RENEWAL-TERM
        int newRenewalCount = policy.getRenewalCount() + 1;
        Date newEffectiveDate = policy.getExpiryDate();
        Date newExpiryDate = addOneYear(newEffectiveDate);

        policy.setPolicyStatus("AC");
        policy.setUwStatus("PN");
        policy.setTotalPremium(newPremium);
        policy.setRenewalCount(newRenewalCount);
        policy.setEffectiveDate(newEffectiveDate);
        policy.setExpiryDate(newExpiryDate);

        policyRepository.updatePolicyForRenewal(policy);

        // 7000-UPDATE-COVERAGES
        policyRepository.updateCoverageDates(policyNumber, newEffectiveDate, newExpiryDate);

        // 8000-SEND-CONFIRMATION
        RenewalResponse response = new RenewalResponse();
        response.setPolicyNumber(policyNumber);
        response.setPreviousPremium(oldPremium);
        response.setNewPremium(newPremium);
        response.setRateChangePct(rateChangePct);
        response.setRateCapped(rateCapped);
        response.setNewEffectiveDate(newEffectiveDate);
        response.setNewExpiryDate(newExpiryDate);
        response.setRenewalCount(newRenewalCount);

        return response;
    }

    /**
     * 3000-CHECK-RENEWAL-ELIGIBILITY: status must be AC or EX.
     */
    private void checkRenewalEligibility(Policy policy) {
        String status = policy.getPolicyStatus();
        if (!"AC".equals(status) && !"EX".equals(status)) {
            throw new IllegalArgumentException("POLICY NOT ELIGIBLE FOR RENEWAL");
        }
    }

    /**
     * 4000-CALCULATE-NEW-PREMIUM: flat 5% rate increase.
     */
    private BigDecimal calculateNewPremium(BigDecimal oldPremium) {
        return oldPremium.multiply(RATE_INCREASE_FACTOR).setScale(2, RoundingMode.DOWN);
    }

    /**
     * 5000-APPLY-RATE-CAP: cap at 15% maximum increase.
     */
    private BigDecimal applyRateCap(BigDecimal oldPremium) {
        return oldPremium.multiply(BigDecimal.ONE.add(RATE_INCREASE_CAP.divide(ONE_HUNDRED, 4, RoundingMode.DOWN)))
                .setScale(2, RoundingMode.DOWN);
    }

    /**
     * Add 1 year using Calendar (mirrors COBOL ADD 10000 to YYYYMMDD).
     */
    private Date addOneYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }
}
