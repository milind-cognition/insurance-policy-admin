package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.PolicyCreationRequest;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PolicyCreationService {

    private static final Logger log = LoggerFactory.getLogger(PolicyCreationService.class);
    private static final String UW_REQUEST_QUEUE = "ACME.PAS.UNDERWRITING.REQUEST";

    private final PolicyRepository policyRepository;
    private final CoverageRepository coverageRepository;
    private final PolicyHolderRepository policyHolderRepository;
    private final JmsTemplate jmsTemplate;

    public PolicyCreationService(PolicyRepository policyRepository,
                                 CoverageRepository coverageRepository,
                                 PolicyHolderRepository policyHolderRepository,
                                 JmsTemplate jmsTemplate) {
        this.policyRepository = policyRepository;
        this.coverageRepository = coverageRepository;
        this.policyHolderRepository = policyHolderRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @Transactional
    public Policy createPolicy(PolicyCreationRequest request) {
        validateRequest(request);

        String policyNumber = generatePolicyNumber();

        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(request.getPolicyType());
        policy.setPolicyStatus(Policy.STATUS_PENDING);
        policy.setEffectiveDate(request.getEffectiveDate());
        policy.setExpiryDate(request.getExpiryDate() != null
                ? request.getExpiryDate()
                : request.getEffectiveDate().plusYears(1));
        policy.setPolicyholderId(request.getPolicyholderId());
        policy.setAgentCode(request.getAgentCode());
        policy.setBranchCode(request.getBranchCode());
        policy.setTotalPremium(request.getTotalPremium() != null
                ? request.getTotalPremium() : BigDecimal.ZERO);
        policy.setDeductible(request.getDeductible() != null
                ? request.getDeductible() : BigDecimal.ZERO);
        policy.setCoverageLimit(request.getCoverageLimit());
        policy.setInceptionDate(request.getEffectiveDate());
        policy.setRenewalCount(0);
        policy.setUwStatus(Policy.UW_PENDING);
        policy.setRiskScore(0);
        policy.setWebIndicator("N");
        policy.setApiFlag("N");
        policy.setLastUpdated(LocalDateTime.now());
        policy.setUpdatedBy("POLNEW");

        policyRepository.save(policy);

        insertDefaultCoverages(policy);

        sendUnderwritingRequest(policyNumber);

        return policy;
    }

    private void validateRequest(PolicyCreationRequest request) {
        if (request.getPolicyholderId() == null || request.getPolicyholderId().isBlank()) {
            throw new PolicyValidationException("Policyholder ID is required");
        }
        if (request.getPolicyType() == null || request.getPolicyType().isBlank()) {
            throw new PolicyValidationException("Policy type is required");
        }
        if (request.getEffectiveDate() != null && request.getEffectiveDate().isBefore(LocalDate.now())) {
            throw new PolicyValidationException("Effective date cannot be in the past");
        }
        if (request.getCoverageLimit() == null || request.getCoverageLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PolicyValidationException("Coverage limit must be greater than zero");
        }
        if (!policyHolderRepository.existsById(request.getPolicyholderId())) {
            throw new PolicyValidationException("Policyholder not found in system");
        }
    }

    private String generatePolicyNumber() {
        Long seq = policyRepository.getNextPolicySequence();
        return String.format("POL%09d", seq);
    }

    private void insertDefaultCoverages(Policy policy) {
        int seq = 0;
        switch (policy.getPolicyType()) {
            case Policy.TYPE_AUTO:
                saveCoverage(policy, ++seq, Coverage.TYPE_AUTL, "Auto Liability");
                saveCoverage(policy, ++seq, Coverage.TYPE_AUTP, "Auto Physical Damage");
                break;
            case Policy.TYPE_HOME:
                saveCoverage(policy, ++seq, Coverage.TYPE_PROP, "Dwelling Coverage");
                saveCoverage(policy, ++seq, Coverage.TYPE_LIAB, "Personal Liability");
                break;
            case Policy.TYPE_COMMERCIAL:
                saveCoverage(policy, ++seq, Coverage.TYPE_PROP, "Commercial Property");
                saveCoverage(policy, ++seq, Coverage.TYPE_LIAB, "General Liability");
                saveCoverage(policy, ++seq, Coverage.TYPE_WKCP, "Workers Compensation");
                break;
            default:
                break;
        }
    }

    private void saveCoverage(Policy policy, int seq, String type, String description) {
        Coverage coverage = new Coverage();
        coverage.setPolicyNumber(policy.getPolicyNumber());
        coverage.setSequenceNum(seq);
        coverage.setCoverageType(type);
        coverage.setDescription(description);
        coverage.setCoverageLimit(policy.getCoverageLimit());
        coverage.setDeductible(policy.getDeductible());
        coverage.setPremium(BigDecimal.ZERO);
        coverage.setEffectiveDate(policy.getEffectiveDate());
        coverage.setExpiryDate(policy.getExpiryDate());
        coverage.setStatus("AC");
        coverageRepository.save(coverage);
    }

    private void sendUnderwritingRequest(String policyNumber) {
        try {
            jmsTemplate.convertAndSend(UW_REQUEST_QUEUE,
                    "NEW_POLICY:" + policyNumber);
        } catch (Exception e) {
            log.warn("Failed to send underwriting request for {}: {}",
                    policyNumber, e.getMessage());
        }
    }
}
