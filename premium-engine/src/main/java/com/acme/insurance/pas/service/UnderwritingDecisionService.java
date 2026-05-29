package com.acme.insurance.pas.service;

import com.acme.insurance.pas.annotation.BusinessRule;
import com.acme.insurance.pas.annotation.RegulatoryRule;
import com.acme.insurance.pas.entity.Policy;
import com.acme.insurance.pas.entity.PolicyHolder;
import com.acme.insurance.pas.entity.UnderwritingDecision;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.UnderwritingDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Underwriting Decision Service - Java 21 replacement for COBOL UNDWRT program
 * (CICS transaction PUWR).
 *
 * Preserves every business rule from UNDWRT.cbl including:
 * - Credit-score-based risk scoring (lines 121-126)
 * - Risk tier adjustment: substandard +200, preferred -100 (lines 128-134)
 * - Claims count thresholds: >3 adds 150, >5 adds 200 (lines 144-149)
 * - Loss ratio > 80% adds 200 (lines 151-159)
 * - High limit surcharges: >$5M +100, >$10M +200 (lines 161-167)
 * - Branch accumulation limit of $500M (lines 170-188)
 * - Decision thresholds: 0-299 AP, 300-599 RS, 600-799 RM, 800+ DC (lines 190-208)
 */
@Service
public class UnderwritingDecisionService {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingDecisionService.class);

    @BusinessRule(
        id = "ACCUM-LIMIT-500M",
        description = "Branch accumulation limit of $500,000,000 for geographic risk concentration",
        sourceProgram = "UNDWRT",
        sourceParagraph = "4000-CHECK-ACCUMULATION",
        lastModified = "2011-07-01"
    )
    private static final BigDecimal ACCUMULATION_LIMIT = new BigDecimal("500000000");

    @BusinessRule(
        id = "HIGH-LIMIT-5M",
        description = "Coverage limit threshold of $5,000,000 for high-limit risk surcharge",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE"
    )
    private static final BigDecimal HIGH_LIMIT_THRESHOLD = new BigDecimal("5000000");

    @BusinessRule(
        id = "HIGH-LIMIT-10M",
        description = "Coverage limit threshold of $10,000,000 for additional high-limit surcharge",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE"
    )
    private static final BigDecimal VERY_HIGH_LIMIT_THRESHOLD = new BigDecimal("10000000");

    private static final String UNDERWRITER_ID = "SYSTEM";

    private final PolicyRepository policyRepository;
    private final UnderwritingDecisionRepository underwritingDecisionRepository;

    public UnderwritingDecisionService(
            PolicyRepository policyRepository,
            UnderwritingDecisionRepository underwritingDecisionRepository) {
        this.policyRepository = policyRepository;
        this.underwritingDecisionRepository = underwritingDecisionRepository;
    }

    /**
     * Result of an underwriting evaluation, mirroring the COBOL working-storage
     * variables WS-RISK-SCORE, WS-DECISION, WS-DECISION-REASON.
     */
    public record UnderwritingResult(
            String policyNumber,
            int riskScore,
            String decisionCode,
            String decisionReason
    ) {}

    @BusinessRule(
        id = "RISK-SCORE-CREDIT",
        description = "Base risk score derived from credit score: (900 - creditScore) / 2. "
            + "If credit score is 0 or null, default risk score is 400.",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE",
        lastModified = "1999-01-20"
    )
    public int calculateBaseRiskScore(Integer creditScore) {
        if (creditScore == null || creditScore <= 0) {
            return 400;
        }
        return (900 - creditScore) / 2;
    }

    @BusinessRule(
        id = "RISK-TIER-ADJUSTMENT",
        description = "Risk tier adjustment: substandard ('U') adds 200, "
            + "preferred ('P') subtracts 100",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE",
        lastModified = "1999-01-20"
    )
    public int applyRiskTierAdjustment(int riskScore, String riskTier) {
        if ("U".equals(riskTier)) {
            riskScore += 200;
        }
        if ("P".equals(riskTier)) {
            riskScore -= 100;
        }
        return riskScore;
    }

    @BusinessRule(
        id = "CLAIMS-COUNT-RISK",
        description = "Claims count adjustment: >3 claims in 5 years adds 150, "
            + ">5 claims adds additional 200 (cumulative with >3 threshold)",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE",
        lastModified = "1999-01-20"
    )
    public int applyClaimsCountAdjustment(int riskScore, int claimCount) {
        if (claimCount > 3) {
            riskScore += 150;
        }
        if (claimCount > 5) {
            riskScore += 200;
        }
        return riskScore;
    }

    @BusinessRule(
        id = "LOSS-RATIO-CHECK",
        description = "Loss ratio check: if (total incurred / total premium) * 100 > 80, "
            + "adds 200 to risk score",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE",
        lastModified = "1999-01-20"
    )
    public int applyLossRatioAdjustment(int riskScore, BigDecimal totalIncurred,
                                        BigDecimal totalPremium) {
        if (totalPremium != null && totalPremium.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal lossRatio = totalIncurred
                    .divide(totalPremium, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (lossRatio.compareTo(new BigDecimal("80")) > 0) {
                riskScore += 200;
            }
        }
        return riskScore;
    }

    @BusinessRule(
        id = "HIGH-LIMIT-SURCHARGE",
        description = "High coverage limit surcharge: >$5M adds 100, >$10M adds additional 200",
        sourceProgram = "UNDWRT",
        sourceParagraph = "3000-CALCULATE-RISK-SCORE"
    )
    public int applyHighLimitAdjustment(int riskScore, BigDecimal coverageLimit) {
        if (coverageLimit != null) {
            if (coverageLimit.compareTo(HIGH_LIMIT_THRESHOLD) > 0) {
                riskScore += 100;
            }
            if (coverageLimit.compareTo(VERY_HIGH_LIMIT_THRESHOLD) > 0) {
                riskScore += 200;
            }
        }
        return riskScore;
    }

    @BusinessRule(
        id = "BRANCH-ACCUMULATION",
        description = "Geographic accumulation check: if total coverage limit for "
            + "all active policies in the same branch exceeds $500M, adds 300 to risk score",
        sourceProgram = "UNDWRT",
        sourceParagraph = "4000-CHECK-ACCUMULATION",
        lastModified = "2011-07-01"
    )
    public int applyAccumulationAdjustment(int riskScore, String branchCode,
                                           StringBuilder reasonBuilder) {
        if (branchCode == null || branchCode.isBlank()) {
            return riskScore;
        }
        BigDecimal accumLimit =
                policyRepository.sumCoverageLimitByBranchCode(branchCode.trim());
        if (accumLimit.compareTo(ACCUMULATION_LIMIT) > 0) {
            riskScore += 300;
            reasonBuilder.append("ACCUMULATION LIMIT EXCEEDED FOR BRANCH ")
                         .append(branchCode.trim());
        }
        return riskScore;
    }

    @RegulatoryRule(
        id = "UW-DECISION-THRESHOLDS",
        description = "Risk score decision thresholds: "
            + "0-299 = Auto-Accept (AP), "
            + "300-599 = Refer to Senior UW (RS), "
            + "600-799 = Refer to Manager (RM), "
            + "800+ = Auto-Decline (DC)",
        sourceProgram = "UNDWRT",
        sourceParagraph = "5000-RENDER-DECISION",
        jurisdiction = "INTERNAL-UW"
    )
    public UnderwritingResult renderDecision(String policyNumber, int riskScore,
                                             String accumulationReason) {
        String decisionCode;
        String decisionReason;

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

        if (accumulationReason != null && !accumulationReason.isEmpty()) {
            decisionReason = accumulationReason;
        }

        return new UnderwritingResult(policyNumber, riskScore,
                decisionCode, decisionReason);
    }

    /**
     * Full underwriting evaluation for a policy, replicating the UNDWRT
     * program flow: 3000-CALCULATE-RISK-SCORE through 5000-RENDER-DECISION.
     *
     * @param policy       the policy to evaluate (must have policyHolder loaded)
     * @param claimCount   number of claims in the last 5 years
     * @param totalIncurred total incurred amount from claims
     */
    public UnderwritingResult evaluate(Policy policy, int claimCount,
                                       BigDecimal totalIncurred) {
        PolicyHolder holder = policy.getPolicyHolder();
        if (holder == null) {
            throw new IllegalArgumentException(
                    "POLICY/CUSTOMER DATA NOT FOUND for " + policy.getPolicyNumber());
        }

        int riskScore = calculateBaseRiskScore(holder.getCreditScore());
        riskScore = applyRiskTierAdjustment(riskScore, holder.getRiskTier());
        riskScore = applyClaimsCountAdjustment(riskScore, claimCount);
        riskScore = applyLossRatioAdjustment(riskScore, totalIncurred,
                policy.getTotalPremium());
        riskScore = applyHighLimitAdjustment(riskScore, policy.getCoverageLimit());

        StringBuilder accumulationReason = new StringBuilder();
        riskScore = applyAccumulationAdjustment(riskScore,
                policy.getBranchCode(), accumulationReason);

        return renderDecision(policy.getPolicyNumber(), riskScore,
                accumulationReason.toString());
    }

    @Transactional
    public UnderwritingDecision persistDecision(UnderwritingResult result) {
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setPolicyNumber(result.policyNumber());
        decision.setDecisionDate(LocalDate.now());
        decision.setDecisionCode(result.decisionCode());
        decision.setRiskScore(result.riskScore());
        decision.setDecisionReason(result.decisionReason());
        decision.setUnderwriterId(UNDERWRITER_ID);
        decision.setCreatedTimestamp(LocalDateTime.now());
        underwritingDecisionRepository.save(decision);

        Policy policy = policyRepository.findById(result.policyNumber())
                .orElseThrow(() -> new IllegalStateException(
                        "Policy not found: " + result.policyNumber()));
        policy.setUwStatus(result.decisionCode());
        policy.setRiskScore(result.riskScore());
        policy.setLastUpdated(LocalDateTime.now());
        policy.setUpdatedBy("UNDWRT");
        policyRepository.save(policy);

        return decision;
    }
}
