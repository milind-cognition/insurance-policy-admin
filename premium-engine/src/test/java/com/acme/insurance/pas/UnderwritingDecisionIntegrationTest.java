package com.acme.insurance.pas;

import com.acme.insurance.pas.entity.Policy;
import com.acme.insurance.pas.entity.UnderwritingDecision;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.UnderwritingDecisionRepository;
import com.acme.insurance.pas.service.UnderwritingDecisionService;
import com.acme.insurance.pas.service.UnderwritingDecisionService.UnderwritingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying underwriting decisions produce identical results
 * to the COBOL UNDWRT program (CICS transaction PUWR).
 *
 * COBOL UNDWRT risk scoring:
 *   Base: (900 - credit_score) / 2, or 400 if no credit score
 *   Substandard tier: +200, Preferred tier: -100
 *   Claims >3: +150, >5: +200 (cumulative)
 *   Loss ratio >80%: +200
 *   Coverage limit >$5M: +100, >$10M: +200 (cumulative)
 *   Branch accumulation >$500M: +300
 *
 * Decision thresholds:
 *   0-299  = AP (Auto-Accept)
 *   300-599 = RS (Refer Senior UW)
 *   600-799 = RM (Refer Manager)
 *   800+   = DC (Auto-Decline)
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UnderwritingDecisionIntegrationTest {

    @Autowired
    private UnderwritingDecisionService underwritingService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UnderwritingDecisionRepository underwritingDecisionRepository;

    @Test
    void creditScoreBasedRiskCalculation() {
        // COBOL: COMPUTE WS-RISK-SCORE = (900 - CUST-CREDIT-SCORE) / 2
        assertEquals(75, underwritingService.calculateBaseRiskScore(750));
        assertEquals(200, underwritingService.calculateBaseRiskScore(500));
        assertEquals(400, underwritingService.calculateBaseRiskScore(100));
        assertEquals(400, underwritingService.calculateBaseRiskScore(0));
        assertEquals(400, underwritingService.calculateBaseRiskScore(null));
    }

    @Test
    void riskTierAdjustment() {
        // COBOL: IF CUST-TIER-SUBSTAND ADD 200 / IF CUST-TIER-PREFERRED SUBTRACT 100
        assertEquals(300, underwritingService.applyRiskTierAdjustment(100, "U"));
        assertEquals(0, underwritingService.applyRiskTierAdjustment(100, "P"));
        assertEquals(100, underwritingService.applyRiskTierAdjustment(100, "S"));
    }

    @Test
    void claimsCountAdjustment() {
        // COBOL: IF WS-CLAIM-COUNT > 3 ADD 150
        //        IF WS-CLAIM-COUNT > 5 ADD 200 (cumulative)
        assertEquals(100, underwritingService.applyClaimsCountAdjustment(100, 2));
        assertEquals(250, underwritingService.applyClaimsCountAdjustment(100, 4));
        assertEquals(450, underwritingService.applyClaimsCountAdjustment(100, 6));
    }

    @Test
    void lossRatioAdjustment() {
        // COBOL: IF WS-LOSS-RATIO > 80 ADD 200 TO WS-RISK-SCORE
        assertEquals(300, underwritingService.applyLossRatioAdjustment(
                100, new BigDecimal("900"), new BigDecimal("1000")));
        assertEquals(100, underwritingService.applyLossRatioAdjustment(
                100, new BigDecimal("500"), new BigDecimal("1000")));
        assertEquals(100, underwritingService.applyLossRatioAdjustment(
                100, new BigDecimal("800"), new BigDecimal("1000")));
    }

    @Test
    void highLimitAdjustment() {
        // COBOL: IF POLICY-LIMIT > 5000000 ADD 100
        //        IF POLICY-LIMIT > 10000000 ADD 200 (cumulative)
        assertEquals(100, underwritingService.applyHighLimitAdjustment(
                100, new BigDecimal("3000000")));
        assertEquals(200, underwritingService.applyHighLimitAdjustment(
                100, new BigDecimal("7000000")));
        assertEquals(400, underwritingService.applyHighLimitAdjustment(
                100, new BigDecimal("15000000")));
    }

    @Test
    void decisionThresholds_autoAccept() {
        // COBOL: WHEN WS-RISK-SCORE < 300 SET DEC-ACCEPT TO TRUE
        UnderwritingResult result = underwritingService.renderDecision("TEST", 150, "");
        assertEquals("AP", result.decisionCode());
        assertEquals("AUTO-ACCEPTED: LOW RISK", result.decisionReason());
    }

    @Test
    void decisionThresholds_referSeniorUW() {
        // COBOL: WHEN WS-RISK-SCORE < 600 SET DEC-REFER-SR TO TRUE
        UnderwritingResult result = underwritingService.renderDecision("TEST", 450, "");
        assertEquals("RS", result.decisionCode());
        assertEquals("REFERRED TO SENIOR UNDERWRITER", result.decisionReason());
    }

    @Test
    void decisionThresholds_referManager() {
        // COBOL: WHEN WS-RISK-SCORE < 800 SET DEC-REFER-MGR TO TRUE
        UnderwritingResult result = underwritingService.renderDecision("TEST", 650, "");
        assertEquals("RM", result.decisionCode());
        assertEquals("REFERRED TO UW MANAGER", result.decisionReason());
    }

    @Test
    void decisionThresholds_autoDecline() {
        // COBOL: WHEN OTHER SET DEC-DECLINE TO TRUE
        UnderwritingResult result = underwritingService.renderDecision("TEST", 850, "");
        assertEquals("DC", result.decisionCode());
        assertEquals("AUTO-DECLINED: HIGH RISK", result.decisionReason());
    }

    @Test
    void decisionThresholdBoundary_299isAccept() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 299, "");
        assertEquals("AP", result.decisionCode());
    }

    @Test
    void decisionThresholdBoundary_300isRefer() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 300, "");
        assertEquals("RS", result.decisionCode());
    }

    @Test
    void decisionThresholdBoundary_599isRefer() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 599, "");
        assertEquals("RS", result.decisionCode());
    }

    @Test
    void decisionThresholdBoundary_600isReferMgr() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 600, "");
        assertEquals("RM", result.decisionCode());
    }

    @Test
    void decisionThresholdBoundary_799isReferMgr() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 799, "");
        assertEquals("RM", result.decisionCode());
    }

    @Test
    void decisionThresholdBoundary_800isDecline() {
        UnderwritingResult result = underwritingService.renderDecision("TEST", 800, "");
        assertEquals("DC", result.decisionCode());
    }

    @Test
    void fullEvaluation_preferredCustomerAutoAccept() {
        // T000000001: credit=750, tier=P -> base=(900-750)/2=75, -100=0(clamped to -25)
        // Actually: 75 - 100 = -25, no claims, no loss, limit <$5M
        // Risk score = -25, which is < 300 => AP
        Policy policy = policyRepository.findById("TEST-AUT-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, BigDecimal.ZERO);

        assertTrue(result.riskScore() < 300,
                "Preferred customer with high credit should auto-accept");
        assertEquals("AP", result.decisionCode());
    }

    @Test
    void fullEvaluation_standardCustomerRefer() {
        // T000000002: credit=500, tier=S -> base=(900-500)/2=200
        // No claims, no loss ratio issue, limit not extreme
        // Risk score = 200 => AP
        Policy policy = policyRepository.findById("TEST-HOM-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, BigDecimal.ZERO);

        assertEquals(200, result.riskScore());
        assertEquals("AP", result.decisionCode());
    }

    @Test
    void fullEvaluation_standardCustomerWithClaims() {
        // T000000002: credit=500, tier=S -> base=200
        // 4 claims -> +150 = 350 => RS (Refer Senior UW)
        Policy policy = policyRepository.findById("TEST-HOM-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 4, BigDecimal.ZERO);

        assertEquals(350, result.riskScore());
        assertEquals("RS", result.decisionCode());
    }

    @Test
    void fullEvaluation_substandardCustomerHighRisk() {
        // T000000003: credit=0, tier=U -> base=400, +200=600
        // coverage limit $2M (not over $5M)
        // Risk score = 600 => RM (Refer Manager)
        Policy policy = policyRepository.findById("TEST-COM-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, BigDecimal.ZERO);

        assertEquals(600, result.riskScore());
        assertEquals("RM", result.decisionCode());
    }

    @Test
    void fullEvaluation_autoDeclineHighLimitLowCredit() {
        // T000000004: credit=100, tier=U -> base=(900-100)/2=400, +200=600
        // coverage limit $15M -> >$5M +100 -> >$10M +200 = 900
        // Risk score = 900 => DC (Auto-Decline)
        Policy policy = policyRepository.findById("TEST-DCL-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, BigDecimal.ZERO);

        assertTrue(result.riskScore() >= 800,
                "Low credit + substandard + high limit should auto-decline");
        assertEquals("DC", result.decisionCode());
    }

    @Test
    void fullEvaluation_lossRatioTriggersRefer() {
        // T000000002: credit=500, tier=S -> base=200
        // Loss ratio: 900/1200 * 100 = 75% (not > 80) -- use 1000/1200 = 83.3% triggers
        // Actually the test policy premium is 1200 so incurred=1000 gives 83.3%
        Policy policy = policyRepository.findById("TEST-HOM-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, new BigDecimal("1000"));

        // base=200, loss ratio 83.3% > 80 adds 200, total = 400 => RS
        assertEquals(400, result.riskScore());
        assertEquals("RS", result.decisionCode());
    }

    @Test
    void persistDecisionUpdatesPolicyAndCreatesAuditRecord() {
        Policy policy = policyRepository.findById("TEST-AUT-001").orElseThrow();
        UnderwritingResult result = underwritingService.evaluate(
                policy, 0, BigDecimal.ZERO);

        UnderwritingDecision saved = underwritingService.persistDecision(result);

        assertNotNull(saved);
        assertEquals(result.decisionCode(), saved.getDecisionCode());
        assertEquals(result.riskScore(), saved.getRiskScore());
        assertEquals("SYSTEM", saved.getUnderwriterId().trim());

        Policy updatedPolicy = policyRepository.findById("TEST-AUT-001").orElseThrow();
        assertEquals(result.decisionCode(), updatedPolicy.getUwStatus().trim());
        assertEquals(result.riskScore(), updatedPolicy.getRiskScore());
        assertEquals("UNDWRT", updatedPolicy.getUpdatedBy().trim());
    }
}
