package com.acme.insurance.pas.service;

import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RenewalService {

    private static final BigDecimal STANDARD_INCREASE = new BigDecimal("1.05");
    private static final BigDecimal RATE_CAP_PCT = new BigDecimal("15.00");
    private static final int MAX_OPEN_CLAIMS_FOR_RENEWAL = 3;

    private final PolicyRepository policyRepository;
    private final CoverageRepository coverageRepository;

    public RenewalService(PolicyRepository policyRepository,
                          CoverageRepository coverageRepository) {
        this.policyRepository = policyRepository;
        this.coverageRepository = coverageRepository;
    }

    @Transactional
    public Policy renewPolicy(String policyNumber) {
        Policy policy = policyRepository.findById(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

        if (!Policy.STATUS_ACTIVE.equals(policy.getPolicyStatus())
                && !Policy.STATUS_EXPIRED.equals(policy.getPolicyStatus())) {
            throw new PolicyValidationException("Policy not eligible for renewal");
        }

        BigDecimal oldPremium = policy.getTotalPremium();
        BigDecimal newPremium = oldPremium.multiply(STANDARD_INCREASE)
                .setScale(2, RoundingMode.HALF_UP);

        newPremium = applyRateCap(oldPremium, newPremium);

        // FIX: The original COBOL (POLRNW line 148) used ADD 10000 to a YYYYMMDD
        // numeric field to add one year, which doesn't handle leap years correctly
        // (e.g., 20240229 + 10000 = 20250229, which is invalid).
        // Java's LocalDate.plusYears() correctly handles this by returning Feb 28.
        LocalDate newEffectiveDate = policy.getExpiryDate();
        LocalDate newExpiryDate = newEffectiveDate.plusYears(1);

        policy.setRenewalCount(policy.getRenewalCount() + 1);
        policy.setEffectiveDate(newEffectiveDate);
        policy.setExpiryDate(newExpiryDate);
        policy.setTotalPremium(newPremium);
        policy.setPolicyStatus(Policy.STATUS_ACTIVE);
        policy.setUwStatus(Policy.UW_PENDING);
        policy.setLastUpdated(LocalDateTime.now());
        policy.setUpdatedBy("POLRNW");

        policyRepository.save(policy);

        coverageRepository.updateDatesByPolicyNumberAndStatus(
                policyNumber, "AC", newEffectiveDate, newExpiryDate);

        return policy;
    }

    private BigDecimal applyRateCap(BigDecimal oldPremium, BigDecimal newPremium) {
        if (oldPremium.compareTo(BigDecimal.ZERO) <= 0) {
            return newPremium;
        }
        BigDecimal increasePercent = newPremium.subtract(oldPremium)
                .divide(oldPremium, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (increasePercent.compareTo(RATE_CAP_PCT) > 0) {
            return oldPremium.multiply(
                    BigDecimal.ONE.add(RATE_CAP_PCT.divide(
                            new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return newPremium;
    }
}
