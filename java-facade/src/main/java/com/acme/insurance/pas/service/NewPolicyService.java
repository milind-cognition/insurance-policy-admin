package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Customer;
import com.acme.insurance.pas.model.NewPolicyRequest;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * New Policy Service - Mirrors COBOL program POLNEW paragraph flow.
 *
 * COBOL paragraph mapping:
 *   1000-INITIALIZE         -> (implicit in createPolicy)
 *   2000-RECEIVE-MAP        -> @RequestBody NewPolicyRequest
 *   3000-VALIDATE-INPUT     -> validateInput()
 *   4000-GENERATE-POLICY-NUM -> generatePolicyNumber()
 *   5000-INSERT-POLICY      -> insertPolicy()
 *   6000-INSERT-COVERAGES   -> insertDefaultCoverages()
 *   7000-SEND-MQ-MESSAGE    -> (logged, not queued)
 *   8000-SEND-CONFIRMATION  -> HTTP 201 response
 */
@Service
public class NewPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(NewPolicyService.class);

    @Autowired
    private PolicyRepository policyRepository;

    /**
     * Creates a new policy following the COBOL POLNEW 0000-MAIN-LOGIC flow.
     *
     * @param request the new policy request data
     * @return the created Policy
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Policy createPolicy(NewPolicyRequest request) {
        // 1000-INITIALIZE (clear state - implicit in stateless REST)

        // 3000-VALIDATE-INPUT
        validateInput(request);

        // 4000-GENERATE-POLICY-NUM
        String policyNumber = generatePolicyNumber();

        // 5000-INSERT-POLICY
        Policy policy = insertPolicy(policyNumber, request);

        // 6000-INSERT-COVERAGES
        insertDefaultCoverages(policyNumber, request);

        // 7000-SEND-MQ-MESSAGE
        logger.info("Underwriting referral queued for policy {}", policyNumber);

        // 8000-SEND-CONFIRMATION (return created policy)
        return policyRepository.findByPolicyNumber(policyNumber);
    }

    /**
     * Mirrors COBOL paragraph 3000-VALIDATE-INPUT.
     * Preserves exact COBOL error messages.
     */
    private void validateInput(NewPolicyRequest request) {
        if (request.getPolicyholderId() == null
                || request.getPolicyholderId().trim().isEmpty()) {
            throw new IllegalArgumentException("POLICYHOLDER ID IS REQUIRED");
        }
        if (request.getPolicyType() == null
                || request.getPolicyType().trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY TYPE IS REQUIRED");
        }
        if (request.getEffectiveDate() != null
                && request.getEffectiveDate().before(new Date())) {
            throw new IllegalArgumentException(
                    "EFFECTIVE DATE CANNOT BE IN PAST");
        }
        if (request.getCoverageLimit() == null
                || request.getCoverageLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "POLICY LIMIT MUST BE GREATER THAN ZERO");
        }
        // Validate policyholder exists in DB2
        Customer customer = policyRepository.findCustomerById(
                request.getPolicyholderId());
        if (customer == null) {
            throw new IllegalArgumentException(
                    "POLICYHOLDER NOT FOUND IN SYSTEM");
        }
    }

    /**
     * Mirrors COBOL paragraph 4000-GENERATE-POLICY-NUM.
     * STRING 'POL' WS-POLICY-SEQ INTO POLICY-NUMBER
     */
    private String generatePolicyNumber() {
        long seq = policyRepository.getNextPolicySequence();
        return String.format("POL%09d", seq);
    }

    /**
     * Mirrors COBOL paragraph 5000-INSERT-POLICY.
     * Sets initial status to 'PN' (Pending), UW status 'PN', risk 0.
     */
    private Policy insertPolicy(String policyNumber, NewPolicyRequest request) {
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(request.getPolicyType());
        policy.setPolicyStatus("PN");
        policy.setEffectiveDate(request.getEffectiveDate());
        policy.setExpiryDate(request.getExpiryDate());
        policy.setPolicyholderId(request.getPolicyholderId());
        policy.setAgentCode(request.getAgentCode());
        policy.setBranchCode(request.getBranchCode());
        policy.setTotalPremium(request.getTotalPremium() != null ?
                request.getTotalPremium() : BigDecimal.ZERO);
        policy.setDeductible(request.getDeductible() != null ?
                request.getDeductible() : BigDecimal.ZERO);
        policy.setCoverageLimit(request.getCoverageLimit());
        policy.setInceptionDate(request.getEffectiveDate());
        policy.setRenewalCount(0);
        policy.setUwStatus("PN");
        policy.setRiskScore(0);
        policy.setWebIndicator("N");
        policy.setApiFlag("N");
        policy.setUpdatedBy("POLNEW");

        policyRepository.insertPolicy(policy);
        return policy;
    }

    /**
     * Mirrors COBOL paragraph 6000-INSERT-COVERAGES.
     * Inserts default coverages based on policy type, matching
     * the COBOL EVALUATE/IF logic exactly.
     */
    private void insertDefaultCoverages(String policyNumber,
                                         NewPolicyRequest request) {
        int seq = 0;
        String policyType = request.getPolicyType();

        // IF POL-TYPE-AUTO
        if ("AUT".equals(policyType)) {
            seq++;
            insertCoverage(policyNumber, seq, "AUTL", "Auto Liability",
                    request);
            seq++;
            insertCoverage(policyNumber, seq, "AUTP",
                    "Auto Physical Damage", request);
        }
        // IF POL-TYPE-HOME
        if ("HOM".equals(policyType)) {
            seq++;
            insertCoverage(policyNumber, seq, "PROP",
                    "Dwelling Coverage", request);
            seq++;
            insertCoverage(policyNumber, seq, "LIAB",
                    "Personal Liability", request);
        }
        // IF POL-TYPE-COMM (COM maps to commercial, COBOL uses 'COM')
        if ("COM".equals(policyType) || "CGL".equals(policyType)) {
            seq++;
            insertCoverage(policyNumber, seq, "PROP",
                    "Commercial Property", request);
            seq++;
            insertCoverage(policyNumber, seq, "LIAB",
                    "General Liability", request);
            seq++;
            insertCoverage(policyNumber, seq, "WKCP",
                    "Workers Compensation", request);
        }
    }

    /**
     * Mirrors COBOL paragraph 6100-WRITE-COVERAGE.
     */
    private void insertCoverage(String policyNumber, int seq,
                                 String coverageType, String description,
                                 NewPolicyRequest request) {
        Coverage coverage = new Coverage();
        coverage.setPolicyNumber(policyNumber);
        coverage.setSequenceNum(seq);
        coverage.setCoverageType(coverageType);
        coverage.setDescription(description);
        coverage.setCoverageLimit(request.getCoverageLimit());
        coverage.setDeductible(request.getDeductible() != null ?
                request.getDeductible() : BigDecimal.ZERO);
        coverage.setPremium(BigDecimal.ZERO);
        coverage.setEffectiveDate(request.getEffectiveDate());
        coverage.setExpiryDate(request.getExpiryDate());
        coverage.setStatus("AC");

        policyRepository.insertCoverage(coverage);
    }
}
