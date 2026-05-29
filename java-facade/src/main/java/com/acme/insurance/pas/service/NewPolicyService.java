package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.NewPolicyRequest;
import com.acme.insurance.pas.model.NewPolicyResponse;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * New Policy Service — migrated from COBOL program POLNEW (CICS transaction PNEW).
 *
 * Paragraph flow:
 *   1000-INITIALIZE      → clearState (implicit)
 *   3000-VALIDATE-INPUT   → validateInput()
 *   4000-GENERATE-POLICY  → repository.generatePolicyNumber()
 *   5000-INSERT-POLICY    → repository.insertPolicy()
 *   6000-INSERT-COVERAGES → insertDefaultCoverages()
 *   7000-SEND-MQ-MESSAGE  → (skipped — logged only)
 *   8000-SEND-CONFIRMATION→ returned as NewPolicyResponse
 */
@Service
public class NewPolicyService {

    private static final Logger log = LoggerFactory.getLogger(NewPolicyService.class);

    @Autowired
    private PolicyRepository policyRepository;

    @Transactional
    public NewPolicyResponse createPolicy(NewPolicyRequest request) {
        // 3000-VALIDATE-INPUT
        validateInput(request);

        // 4000-GENERATE-POLICY-NUM
        String policyNumber = policyRepository.generatePolicyNumber();

        // 5000-INSERT-POLICY
        Policy policy = buildPolicy(policyNumber, request);
        policyRepository.insertPolicy(policy);

        // 6000-INSERT-COVERAGES
        int coveragesCreated = insertDefaultCoverages(policyNumber, request);

        // 7000-SEND-MQ-MESSAGE (skipped in Java — MQ underwriting referral)
        log.info("POLNEW: Skipping MQ underwriting referral for policy {}", policyNumber);

        // 8000-SEND-CONFIRMATION
        NewPolicyResponse response = new NewPolicyResponse();
        response.setPolicyNumber(policyNumber);
        response.setPolicyStatus("PN");
        response.setMessage("Policy created successfully");
        response.setCoveragesCreated(coveragesCreated);
        return response;
    }

    private void validateInput(NewPolicyRequest request) {
        if (request.getPolicyholderId() == null || request.getPolicyholderId().trim().isEmpty()) {
            throw new IllegalArgumentException("POLICYHOLDER ID IS REQUIRED");
        }
        if (request.getPolicyType() == null || request.getPolicyType().trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY TYPE IS REQUIRED");
        }
        if (request.getEffectiveDate() != null && !request.getEffectiveDate().trim().isEmpty()) {
            Date effDate = parseDate(request.getEffectiveDate());
            Date today = truncateToDay(new Date());
            if (effDate.before(today)) {
                throw new IllegalArgumentException("EFFECTIVE DATE CANNOT BE IN PAST");
            }
        }
        if (request.getCoverageLimit() == null || request.getCoverageLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("POLICY LIMIT MUST BE GREATER THAN ZERO");
        }
        if (!policyRepository.customerExists(request.getPolicyholderId())) {
            throw new IllegalArgumentException("POLICYHOLDER NOT FOUND IN SYSTEM");
        }
    }

    private Policy buildPolicy(String policyNumber, NewPolicyRequest request) {
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(request.getPolicyType());
        policy.setPolicyStatus("PN");
        policy.setEffectiveDate(isPresent(request.getEffectiveDate()) ?
                parseDate(request.getEffectiveDate()) : new Date());
        policy.setExpiryDate(isPresent(request.getExpiryDate()) ?
                parseDate(request.getExpiryDate()) : addOneYear(policy.getEffectiveDate()));
        policy.setPolicyholderId(request.getPolicyholderId());
        policy.setAgentCode(request.getAgentCode());
        policy.setBranchCode(request.getBranchCode());
        policy.setTotalPremium(request.getTotalPremium() != null ?
                request.getTotalPremium() : BigDecimal.ZERO);
        policy.setDeductible(request.getDeductible() != null ?
                request.getDeductible() : BigDecimal.ZERO);
        policy.setCoverageLimit(request.getCoverageLimit());
        policy.setInceptionDate(policy.getEffectiveDate());
        policy.setRenewalCount(0);
        policy.setUwStatus("PN");
        policy.setRiskScore(0);
        policy.setWebIndicator("N");
        policy.setApiFlag("N");
        policy.setUpdatedBy("POLNEW");
        return policy;
    }

    private int insertDefaultCoverages(String policyNumber, NewPolicyRequest request) {
        String policyType = request.getPolicyType().toUpperCase();
        Date effDate = isPresent(request.getEffectiveDate()) ?
                parseDate(request.getEffectiveDate()) : new Date();
        Date expDate = isPresent(request.getExpiryDate()) ?
                parseDate(request.getExpiryDate()) : addOneYear(effDate);

        int count = 0;
        if ("AUT".equals(policyType)) {
            insertCoverage(policyNumber, 1, "AUTL", "Auto Liability", effDate, expDate);
            insertCoverage(policyNumber, 2, "AUTP", "Auto Physical Damage", effDate, expDate);
            count = 2;
        } else if ("HOM".equals(policyType)) {
            insertCoverage(policyNumber, 1, "PROP", "Dwelling Coverage", effDate, expDate);
            insertCoverage(policyNumber, 2, "LIAB", "Personal Liability", effDate, expDate);
            count = 2;
        } else if ("COM".equals(policyType)) {
            insertCoverage(policyNumber, 1, "PROP", "Commercial Property", effDate, expDate);
            insertCoverage(policyNumber, 2, "LIAB", "General Liability", effDate, expDate);
            insertCoverage(policyNumber, 3, "WKCP", "Workers Compensation", effDate, expDate);
            count = 3;
        }
        return count;
    }

    private void insertCoverage(String policyNumber, int seqNum, String type,
                                String description, Date effDate, Date expDate) {
        Coverage coverage = new Coverage();
        coverage.setPolicyNumber(policyNumber);
        coverage.setSequenceNum(seqNum);
        coverage.setCoverageType(type);
        coverage.setDescription(description);
        coverage.setCoverageLimit(BigDecimal.ZERO);
        coverage.setDeductible(BigDecimal.ZERO);
        coverage.setPremium(BigDecimal.ZERO);
        coverage.setEffectiveDate(effDate);
        coverage.setExpiryDate(expDate);
        coverage.setStatus("AC");
        policyRepository.insertCoverage(coverage);
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr + ". Expected YYYY-MM-DD");
        }
    }

    private Date truncateToDay(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(sdf.format(date));
        } catch (ParseException e) {
            return date;
        }
    }

    @SuppressWarnings("deprecation")
    private Date addOneYear(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.add(java.util.Calendar.YEAR, 1);
        return cal.getTime();
    }
}
