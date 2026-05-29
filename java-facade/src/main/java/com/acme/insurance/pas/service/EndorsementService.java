package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.EndorsementRequest;
import com.acme.insurance.pas.model.EndorsementResponse;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Endorsement Service - migrated from COBOL program POLEND (CICS transaction PEND).
 *
 * Mirrors the COBOL paragraph flow:
 *   1000-INITIALIZE          -> processEndorsement entry (clear state, get date)
 *   2000-RECEIVE-ENDORSEMENT -> read request + fetch policy from DB
 *   3000-VALIDATE-ENDORSEMENT -> validateEndorsement()
 *   4000-CALCULATE-PRORATA   -> calculateProrata()
 *   5000-APPLY-ENDORSEMENT   -> applyEndorsement()
 *   6000-WRITE-AUDIT-TRAIL   -> writeAuditTrail()
 *   7000-SEND-CONFIRMATION   -> build and return EndorsementResponse
 */
@Service
public class EndorsementService {

    private static final Logger logger = LoggerFactory.getLogger(EndorsementService.class);

    @Autowired
    private PolicyRepository policyRepository;

    /**
     * Process an endorsement request.
     *
     * @param request the endorsement request
     * @return EndorsementResponse on success, null if policy not found
     * @throws IllegalArgumentException if validation fails (mirrors COBOL error messages)
     */
    @Transactional
    public EndorsementResponse processEndorsement(EndorsementRequest request) {
        // 1000-INITIALIZE
        LocalDate currentDate = LocalDate.now();

        // 2000-RECEIVE-ENDORSEMENT
        Policy policy = policyRepository.findByPolicyNumber(request.getPolicyNumber());
        if (policy == null) {
            return null;
        }

        // 3000-VALIDATE-ENDORSEMENT
        validateEndorsement(policy, request);

        // 4000-CALCULATE-PRORATA
        BigDecimal prorataFactor = calculateProrata(policy, currentDate);

        // 5000-APPLY-ENDORSEMENT
        BigDecimal adjustedPremium = request.getPremiumAdjustment()
                .multiply(prorataFactor)
                .setScale(2, RoundingMode.HALF_UP);

        int endorsementSeq = applyEndorsement(request, policy, adjustedPremium, currentDate);

        BigDecimal newTotalPremium = policy.getTotalPremium().add(adjustedPremium);

        // 6000-WRITE-AUDIT-TRAIL
        writeAuditTrail(request.getPolicyNumber(), endorsementSeq);

        // 7000-SEND-CONFIRMATION
        EndorsementResponse response = new EndorsementResponse();
        response.setPolicyNumber(request.getPolicyNumber());
        response.setEndorsementSeq(endorsementSeq);
        response.setEndorsementType(request.getEndorsementType());
        response.setPremiumAdjustment(adjustedPremium);
        response.setProrataFactor(prorataFactor);
        response.setNewTotalPremium(newTotalPremium);
        return response;
    }

    /**
     * 3000-VALIDATE-ENDORSEMENT
     */
    private void validateEndorsement(Policy policy, EndorsementRequest request) {
        if (!"AC".equals(policy.getPolicyStatus())) {
            throw new IllegalArgumentException("ONLY ACTIVE POLICIES CAN BE ENDORSED");
        }
        if (request.getEndorsementType() == null
                || request.getEndorsementType().trim().isEmpty()) {
            throw new IllegalArgumentException("ENDORSEMENT TYPE IS REQUIRED");
        }
        if (request.getPremiumAdjustment() == null) {
            throw new IllegalArgumentException("PREMIUM ADJUSTMENT IS REQUIRED");
        }
    }

    /**
     * 4000-CALCULATE-PRORATA
     */
    private BigDecimal calculateProrata(Policy policy, LocalDate currentDate) {
        LocalDate effectiveDate = toLocalDate(policy.getEffectiveDate());
        LocalDate expiryDate = toLocalDate(policy.getExpiryDate());

        long daysInTerm = ChronoUnit.DAYS.between(effectiveDate, expiryDate);
        long daysRemaining = ChronoUnit.DAYS.between(currentDate, expiryDate);

        if (daysRemaining <= 0) {
            throw new IllegalArgumentException("POLICY TERM HAS EXPIRED - ENDORSEMENT NOT ALLOWED");
        }

        if (daysInTerm <= 0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.valueOf(daysRemaining)
                .divide(BigDecimal.valueOf(daysInTerm), 6, RoundingMode.HALF_UP);
    }

    /**
     * 5000-APPLY-ENDORSEMENT
     */
    private int applyEndorsement(EndorsementRequest request, Policy policy,
                                 BigDecimal adjustedPremium, LocalDate currentDate) {
        int endorsementSeq = policyRepository.getNextEndorsementSeq(request.getPolicyNumber());

        Endorsement endorsement = new Endorsement();
        endorsement.setPolicyNumber(request.getPolicyNumber());
        endorsement.setEndorsementSeq(endorsementSeq);
        endorsement.setEndorsementType(request.getEndorsementType());
        endorsement.setEffectiveDate(toDate(currentDate));
        endorsement.setDescription(request.getDescription());
        endorsement.setPremiumAdjustment(adjustedPremium);
        endorsement.setProcessedDate(new Date());
        endorsement.setProcessedBy("POLEND");

        policyRepository.insertEndorsement(endorsement);

        policyRepository.updatePolicyPremium(request.getPolicyNumber(), adjustedPremium);

        return endorsementSeq;
    }

    /**
     * 6000-WRITE-AUDIT-TRAIL
     */
    private void writeAuditTrail(String policyNumber, int endorsementSeq) {
        logger.info("Endorsement audit: policy {}, seq {}", policyNumber, endorsementSeq);
    }

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return new java.sql.Date(date.getTime()).toLocalDate();
    }

    private static Date toDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }
}
