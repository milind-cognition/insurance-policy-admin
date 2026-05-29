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
 * Policy Renewal Service — Java translation of COBOL program POLRNW.
 *
 * Replicates the paragraph flow:
 *   1000-INITIALIZE
 *   2000-READ-EXISTING-POLICY
 *   3000-CHECK-RENEWAL-ELIGIBILITY
 *   4000-CALCULATE-NEW-PREMIUM
 *   5000-APPLY-RATE-CAP
 *   6000-CREATE-RENEWAL-TERM
 *   7000-UPDATE-COVERAGES
 *   8000-SEND-CONFIRMATION (returns RenewalResponse)
 *
 * @author POLRNW Migration
 */
@Service
public class PolicyRenewalService {

    private static final BigDecimal STANDARD_RATE_INCREASE = new BigDecimal("1.05");
    private static final BigDecimal RATE_INCREASE_CAP = new BigDecimal("15.00");
    private static final BigDecimal CAP_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private PolicyRepository policyRepository;

    @Transactional
    public RenewalResponse renewPolicy(String policyNumber) {
        // 2000-READ-EXISTING-POLICY
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            throw new IllegalArgumentException("POLICY NOT FOUND FOR RENEWAL");
        }

        // 3000-CHECK-RENEWAL-ELIGIBILITY
        String status = policy.getPolicyStatus();
        if (!"AC".equals(status) && !"EX".equals(status)) {
            throw new IllegalArgumentException("POLICY NOT ELIGIBLE FOR RENEWAL");
        }

        BigDecimal oldPremium = policy.getTotalPremium();

        // 4000-CALCULATE-NEW-PREMIUM
        BigDecimal newPremium = oldPremium.multiply(STANDARD_RATE_INCREASE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal rateChangePct = newPremium.subtract(oldPremium)
                .multiply(HUNDRED)
                .divide(oldPremium, 2, RoundingMode.HALF_UP);

        // 5000-APPLY-RATE-CAP
        boolean rateCapped = false;
        if (rateChangePct.compareTo(RATE_INCREASE_CAP) > 0) {
            newPremium = oldPremium.multiply(CAP_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_UP);
            rateChangePct = RATE_INCREASE_CAP;
            rateCapped = true;
        }

        // 6000-CREATE-RENEWAL-TERM
        int newRenewalCount = policy.getRenewalCount() + 1;
        Date newEffectiveDate = policy.getExpiryDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(newEffectiveDate);
        cal.add(Calendar.YEAR, 1);
        Date newExpiryDate = cal.getTime();

        policy.setPolicyStatus("AC");
        policy.setEffectiveDate(newEffectiveDate);
        policy.setExpiryDate(newExpiryDate);
        policy.setTotalPremium(newPremium);
        policy.setRenewalCount(newRenewalCount);
        policy.setUwStatus("PN");
        policy.setUpdatedBy("POLRNW");

        policyRepository.updatePolicyRenewal(policy);

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
        response.setMessage("POLICY RENEWAL PROCESSED SUCCESSFULLY");

        return response;
    }
}
