package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.EndorsementRequest;
import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.EndorsementRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class EndorsementService {

    private final PolicyRepository policyRepository;
    private final EndorsementRepository endorsementRepository;
    private final EntityManager entityManager;

    public EndorsementService(PolicyRepository policyRepository,
                              EndorsementRepository endorsementRepository,
                              EntityManager entityManager) {
        this.policyRepository = policyRepository;
        this.endorsementRepository = endorsementRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Endorsement processEndorsement(EndorsementRequest request) {
        Policy policy = policyRepository.findById(request.getPolicyNumber())
                .orElseThrow(() -> new PolicyNotFoundException(request.getPolicyNumber()));

        entityManager.lock(policy, LockModeType.PESSIMISTIC_WRITE);

        if (!Policy.STATUS_ACTIVE.equals(policy.getPolicyStatus())) {
            throw new PolicyValidationException("Only active policies can be endorsed");
        }
        if (request.getEndorsementType() == null || request.getEndorsementType().isBlank()) {
            throw new PolicyValidationException("Endorsement type is required");
        }

        BigDecimal proRataFactor = calculateProRataFactor(policy);

        int nextSeq = endorsementRepository.findMaxEndorsementSeqByPolicyNumber(
                request.getPolicyNumber()) + 1;

        Endorsement endorsement = new Endorsement();
        endorsement.setPolicyNumber(request.getPolicyNumber());
        endorsement.setEndorsementSeq(nextSeq);
        endorsement.setEndorsementType(request.getEndorsementType());
        endorsement.setEffectiveDate(LocalDate.now());
        endorsement.setDescription(request.getDescription());
        endorsement.setPremiumAdjustment(request.getPremiumAdjustment() != null
                ? request.getPremiumAdjustment() : BigDecimal.ZERO);
        endorsement.setProcessedDate(LocalDateTime.now());
        endorsement.setProcessedBy("POLEND");

        endorsementRepository.save(endorsement);

        if (request.getPremiumAdjustment() != null) {
            policy.setTotalPremium(
                    policy.getTotalPremium().add(request.getPremiumAdjustment()));
        }
        policy.setLastUpdated(LocalDateTime.now());
        policy.setUpdatedBy("POLEND");
        policyRepository.save(policy);

        return endorsement;
    }

    private BigDecimal calculateProRataFactor(Policy policy) {
        LocalDate today = LocalDate.now();
        long daysInTerm = ChronoUnit.DAYS.between(
                policy.getEffectiveDate(), policy.getExpiryDate());
        long daysRemaining = ChronoUnit.DAYS.between(today, policy.getExpiryDate());

        if (daysInTerm <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(daysRemaining)
                .divide(BigDecimal.valueOf(daysInTerm), 6, RoundingMode.HALF_UP);
    }
}
