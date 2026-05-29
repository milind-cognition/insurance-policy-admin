package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Customer;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.UnderwritingDecision;
import com.acme.insurance.pas.model.UnderwritingResponse;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Underwriting Service - Java equivalent of UNDWRT.cbl.
 *
 * Mirrors the COBOL paragraph flow:
 *   1000-INITIALIZE
 *   2000-READ-POLICY-DATA
 *   3000-CALCULATE-RISK-SCORE
 *   4000-CHECK-ACCUMULATION
 *   5000-RENDER-DECISION
 *   6000-WRITE-DECISION
 *   7000-SEND-MQ-NOTIFICATION (logged only)
 *   8000-DISPLAY-RESULT
 *
 * CICS Transaction: PUWR
 */
@Service
public class UnderwritingService {

    private static final Logger LOG = LoggerFactory.getLogger(UnderwritingService.class);

    private static final int SCORE_AUTO_ACCEPT = 300;
    private static final int SCORE_REFER_SR = 600;
    private static final int SCORE_REFER_MGR = 800;
    private static final BigDecimal ACCUM_LIMIT = new BigDecimal("500000000");

    @Autowired
    private PolicyRepository policyRepository;

    /**
     * Evaluate underwriting risk for a policy.
     *
     * @param policyNumber the policy to evaluate
     * @return response with risk score and decision, or null if policy not found
     * @throws IllegalArgumentException if policyNumber is blank
     */
    @Transactional
    public UnderwritingResponse evaluateRisk(String policyNumber) {
        // 1000-INITIALIZE
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy number is required");
        }
        int riskScore = 0;
        String decisionReason = "";

        // 2000-READ-POLICY-DATA
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null;
        }
        Customer customer = policyRepository.findCustomerById(
                policy.getPolicyholderId());
        if (customer == null) {
            return null;
        }

        // 3000-CALCULATE-RISK-SCORE
        // 3a. Base score from credit
        int creditScore = customer.getCreditScore();
        if (creditScore > 0) {
            riskScore = (900 - creditScore) / 2;
        } else {
            riskScore = 400;
        }

        // 3b. Risk tier adjustment
        String riskTier = customer.getRiskTier();
        if ("U".equals(riskTier)) {
            riskScore += 200;
        }
        if ("P".equals(riskTier)) {
            riskScore -= 100;
        }

        // 3c. Loss history
        Map<String, Object> claimHistory = policyRepository.getClaimHistory(
                policyNumber);
        int claimCount = ((Number) claimHistory.get("CLAIM_COUNT")).intValue();
        BigDecimal totalIncurred = (BigDecimal) claimHistory.get("TOTAL_INCURRED");

        if (claimCount > 3) {
            riskScore += 150;
        }
        if (claimCount > 5) {
            riskScore += 200;
        }

        // 3d. Loss ratio
        BigDecimal lossRatio = BigDecimal.ZERO;
        BigDecimal totalPremium = policy.getTotalPremium();
        if (totalPremium != null && totalPremium.compareTo(BigDecimal.ZERO) > 0) {
            lossRatio = totalIncurred.multiply(new BigDecimal("100"))
                    .divide(totalPremium, 2, BigDecimal.ROUND_HALF_UP);
            if (lossRatio.compareTo(new BigDecimal("80")) > 0) {
                riskScore += 200;
            }
        }

        // 3e. High limit surcharge
        BigDecimal coverageLimit = policy.getCoverageLimit();
        if (coverageLimit != null) {
            if (coverageLimit.compareTo(new BigDecimal("5000000")) > 0) {
                riskScore += 100;
            }
            if (coverageLimit.compareTo(new BigDecimal("10000000")) > 0) {
                riskScore += 200;
            }
        }

        // 4000-CHECK-ACCUMULATION
        BigDecimal accumulatedLimit = BigDecimal.ZERO;
        String branchCode = policy.getBranchCode();
        if (branchCode != null && !branchCode.trim().isEmpty()) {
            accumulatedLimit = policyRepository.getAccumulatedLimit(branchCode);
            if (accumulatedLimit.compareTo(ACCUM_LIMIT) > 0) {
                riskScore += 300;
                decisionReason = "ACCUMULATION LIMIT EXCEEDED FOR BRANCH "
                        + branchCode;
            }
        }

        // 5000-RENDER-DECISION
        String decisionCode;
        if (riskScore < SCORE_AUTO_ACCEPT) {
            decisionCode = "AP";
            decisionReason = "AUTO-ACCEPTED: LOW RISK";
        } else if (riskScore < SCORE_REFER_SR) {
            decisionCode = "RS";
            decisionReason = "REFERRED TO SENIOR UNDERWRITER";
        } else if (riskScore < SCORE_REFER_MGR) {
            decisionCode = "RM";
            decisionReason = "REFERRED TO UW MANAGER";
        } else {
            decisionCode = "DC";
            decisionReason = "AUTO-DECLINED: HIGH RISK";
        }

        // 6000-WRITE-DECISION
        Date now = new Date();
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setPolicyNumber(policyNumber);
        decision.setDecisionDate(now);
        decision.setDecisionCode(decisionCode);
        decision.setRiskScore(riskScore);
        decision.setDecisionReason(decisionReason);
        decision.setUnderwriterId("SYSTEM");
        decision.setCreatedTimestamp(new Timestamp(now.getTime()));
        policyRepository.insertUnderwritingDecision(decision);

        policyRepository.updatePolicyUnderwriting(policyNumber,
                decisionCode, riskScore);

        // 7000-SEND-MQ-NOTIFICATION (just log in Java)
        LOG.info("UW decision for {}: code={}, score={}, reason={}",
                policyNumber, decisionCode, riskScore, decisionReason);

        // 8000-DISPLAY-RESULT
        UnderwritingResponse response = new UnderwritingResponse();
        response.setPolicyNumber(policyNumber);
        response.setRiskScore(riskScore);
        response.setDecisionCode(decisionCode);
        response.setDecisionReason(decisionReason);
        response.setClaimCount(claimCount);
        response.setLossRatio(lossRatio);
        response.setAccumulatedLimit(accumulatedLimit);
        return response;
    }
}
