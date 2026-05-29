package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.UnderwritingResult;
import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.PolicyHolder;
import com.acme.insurance.pas.model.UnderwritingDecision;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.UnderwritingDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UnderwritingService {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingService.class);
    private static final String UW_DECISION_QUEUE = "ACME.PAS.UNDERWRITING.DECISION";
    private static final BigDecimal ACCUMULATION_LIMIT = new BigDecimal("500000000");
    private static final BigDecimal HIGH_LIMIT_TIER1 = new BigDecimal("5000000");
    private static final BigDecimal HIGH_LIMIT_TIER2 = new BigDecimal("10000000");

    private final PolicyRepository policyRepository;
    private final PolicyHolderRepository policyHolderRepository;
    private final UnderwritingDecisionRepository uwDecisionRepository;
    private final JmsTemplate jmsTemplate;

    public UnderwritingService(PolicyRepository policyRepository,
                               PolicyHolderRepository policyHolderRepository,
                               UnderwritingDecisionRepository uwDecisionRepository,
                               JmsTemplate jmsTemplate) {
        this.policyRepository = policyRepository;
        this.policyHolderRepository = policyHolderRepository;
        this.uwDecisionRepository = uwDecisionRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @Transactional
    public UnderwritingResult evaluateRisk(String policyNumber) {
        Policy policy = policyRepository.findById(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

        PolicyHolder holder = policyHolderRepository.findById(policy.getPolicyholderId())
                .orElseThrow(() -> new PolicyNotFoundException(
                        "Policyholder not found for policy: " + policyNumber));

        int riskScore = calculateRiskScore(policy, holder);

        riskScore = checkAccumulation(policy, riskScore);

        String decisionCode;
        String decisionReason;

        if (riskScore < 300) {
            decisionCode = UnderwritingDecision.DECISION_ACCEPT;
            decisionReason = "AUTO-ACCEPTED: LOW RISK";
        } else if (riskScore < 600) {
            decisionCode = UnderwritingDecision.DECISION_REFER_SENIOR;
            decisionReason = "REFERRED TO SENIOR UNDERWRITER";
        } else if (riskScore < 800) {
            decisionCode = UnderwritingDecision.DECISION_REFER_MANAGER;
            decisionReason = "REFERRED TO UW MANAGER";
        } else {
            decisionCode = UnderwritingDecision.DECISION_DECLINE;
            decisionReason = "AUTO-DECLINED: HIGH RISK";
        }

        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setPolicyNumber(policyNumber);
        decision.setDecisionDate(LocalDate.now());
        decision.setDecisionCode(decisionCode);
        decision.setRiskScore(riskScore);
        decision.setDecisionReason(decisionReason);
        decision.setUnderwriterId("SYSTEM");
        decision.setCreatedTimestamp(LocalDateTime.now());
        uwDecisionRepository.save(decision);

        policy.setUwStatus(decisionCode);
        policy.setRiskScore(riskScore);
        policy.setLastUpdated(LocalDateTime.now());
        policy.setUpdatedBy("UNDWRT");
        policyRepository.save(policy);

        sendDecisionNotification(policyNumber, decisionCode);

        return new UnderwritingResult(policyNumber, riskScore, decisionCode, decisionReason);
    }

    private int calculateRiskScore(Policy policy, PolicyHolder holder) {
        int score;

        if (holder.getCreditScore() > 0) {
            score = (900 - holder.getCreditScore()) / 2;
        } else {
            score = 400;
        }

        if (PolicyHolder.RISK_TIER_SUBSTANDARD.equals(holder.getRiskTier())) {
            score += 200;
        }
        if (PolicyHolder.RISK_TIER_PREFERRED.equals(holder.getRiskTier())) {
            score -= 100;
        }

        if (policy.getCoverageLimit().compareTo(HIGH_LIMIT_TIER1) > 0) {
            score += 100;
        }
        if (policy.getCoverageLimit().compareTo(HIGH_LIMIT_TIER2) > 0) {
            score += 200;
        }

        return Math.max(score, 0);
    }

    private int checkAccumulation(Policy policy, int riskScore) {
        if (policy.getBranchCode() == null) {
            return riskScore;
        }
        BigDecimal accumLimit = policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(
                policy.getBranchCode(), Policy.STATUS_ACTIVE);
        if (accumLimit != null && accumLimit.compareTo(ACCUMULATION_LIMIT) > 0) {
            riskScore += 300;
        }
        return riskScore;
    }

    private void sendDecisionNotification(String policyNumber, String decisionCode) {
        try {
            jmsTemplate.convertAndSend(UW_DECISION_QUEUE,
                    "UW_DECISION:" + policyNumber + ":" + decisionCode);
        } catch (Exception e) {
            log.warn("Failed to send UW decision for {}: {}",
                    policyNumber, e.getMessage());
        }
    }
}
