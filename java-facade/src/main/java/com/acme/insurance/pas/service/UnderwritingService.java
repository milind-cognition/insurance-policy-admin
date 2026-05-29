package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.UnderwritingDecision;
import com.acme.insurance.pas.model.UnderwritingResponse;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.PolicyRepository.PolicyCustomerData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Underwriting evaluation service.
 * Migrated from COBOL program UNDWRT (CICS transaction PUWR).
 *
 * Replicates the mainframe risk-scoring and decision logic:
 *   1000-INITIALIZE
 *   2000-READ-POLICY-DATA
 *   3000-CALCULATE-RISK-SCORE
 *   4000-CHECK-ACCUMULATION
 *   5000-RENDER-DECISION
 *   6000-WRITE-DECISION
 */
@Service
public class UnderwritingService {

    private static final BigDecimal ACCUMULATION_LIMIT = new BigDecimal("500000000");
    private static final BigDecimal HIGH_LIMIT_TIER1 = new BigDecimal("5000000");
    private static final BigDecimal HIGH_LIMIT_TIER2 = new BigDecimal("10000000");

    @Autowired
    private PolicyRepository policyRepository;

    @Transactional
    public UnderwritingResponse evaluate(String policyNumber) {

        // 1000-INITIALIZE
        int riskScore = 0;
        String decisionReason = "";

        // 2000-READ-POLICY-DATA
        PolicyCustomerData data = policyRepository.findPolicyWithCustomer(policyNumber);
        if (data == null) {
            throw new IllegalArgumentException("POLICY/CUSTOMER DATA NOT FOUND");
        }

        // 3000-CALCULATE-RISK-SCORE
        int creditScore = data.getCreditScore();
        if (!data.isCreditScoreNull() && creditScore > 0) {
            riskScore = (900 - creditScore) / 2;
        } else {
            riskScore = 400;
        }

        String riskTier = data.getRiskTier();
        if ("U".equals(riskTier)) {
            riskScore += 200;
        } else if ("P".equals(riskTier)) {
            riskScore -= 100;
        }

        long[] claimStats = policyRepository.getClaimStats(policyNumber);
        long claimCount = claimStats[0];
        long totalIncurred = claimStats[1];

        if (claimCount > 3) {
            riskScore += 150;
        }
        if (claimCount > 5) {
            riskScore += 200;
        }

        BigDecimal premium = data.getTotalPremium();
        if (premium != null && premium.compareTo(BigDecimal.ZERO) > 0) {
            long lossRatio = BigDecimal.valueOf(totalIncurred)
                    .multiply(new BigDecimal(100))
                    .divide(premium, 0, BigDecimal.ROUND_DOWN)
                    .longValue();
            if (lossRatio > 80) {
                riskScore += 200;
            }
        }

        BigDecimal coverageLimit = data.getCoverageLimit();
        if (coverageLimit != null) {
            if (coverageLimit.compareTo(HIGH_LIMIT_TIER1) > 0) {
                riskScore += 100;
            }
            if (coverageLimit.compareTo(HIGH_LIMIT_TIER2) > 0) {
                riskScore += 200;
            }
        }

        // 4000-CHECK-ACCUMULATION
        String branchCode = data.getBranchCode();
        if (branchCode != null) {
            BigDecimal branchTotal = policyRepository.getBranchAccumulation(branchCode);
            if (branchTotal != null && branchTotal.compareTo(ACCUMULATION_LIMIT) > 0) {
                riskScore += 300;
                decisionReason = "ACCUMULATION LIMIT EXCEEDED FOR BRANCH " + branchCode;
            }
        }

        // 5000-RENDER-DECISION
        String decisionCode;
        String accumulationReason = decisionReason;
        if (riskScore < 300) {
            decisionCode = "AP";
            decisionReason = "AUTO-ACCEPTED: LOW RISK";
        } else if (riskScore < 600) {
            decisionCode = "RS";
            decisionReason = "REFERRED TO SENIOR UNDERWRITER";
        } else if (riskScore < 800) {
            decisionCode = "RM";
            decisionReason = "REFERRED TO UW MANAGER";
        } else {
            decisionCode = "DC";
            decisionReason = "AUTO-DECLINED: HIGH RISK";
        }
        if (!accumulationReason.isEmpty()) {
            decisionReason = accumulationReason + " - " + decisionReason;
        }

        // 6000-WRITE-DECISION
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setPolicyNumber(policyNumber);
        decision.setDecisionDate(new Date());
        decision.setDecisionCode(decisionCode);
        decision.setRiskScore(riskScore);
        decision.setDecisionReason(decisionReason);
        decision.setUnderwriterId("UNDWRT");

        policyRepository.insertUnderwritingDecision(decision);
        policyRepository.updatePolicyUwStatus(policyNumber, decisionCode, riskScore);

        // Build response
        UnderwritingResponse response = new UnderwritingResponse();
        response.setPolicyNumber(policyNumber);
        response.setRiskScore(riskScore);
        response.setDecisionCode(decisionCode);
        response.setDecisionReason(decisionReason);
        response.setMessage("UNDERWRITING EVALUATION COMPLETE");
        return response;
    }
}
