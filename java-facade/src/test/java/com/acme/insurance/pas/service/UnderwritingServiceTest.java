package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.UnderwritingResult;
import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.PolicyHolder;
import com.acme.insurance.pas.model.UnderwritingDecision;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.UnderwritingDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnderwritingServiceTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private PolicyHolderRepository policyHolderRepository;
    @Mock
    private UnderwritingDecisionRepository uwDecisionRepository;
    @Mock
    private JmsTemplate jmsTemplate;

    private UnderwritingService service;

    @BeforeEach
    void setUp() {
        service = new UnderwritingService(policyRepository, policyHolderRepository,
                uwDecisionRepository, jmsTemplate);
    }

    private Policy buildPolicy(BigDecimal coverageLimit, String branchCode) {
        Policy p = new Policy();
        p.setPolicyNumber("POL000000001");
        p.setPolicyType("AUT");
        p.setPolicyStatus("AC");
        p.setEffectiveDate(LocalDate.now().minusMonths(6));
        p.setExpiryDate(LocalDate.now().plusMonths(6));
        p.setCoverageLimit(coverageLimit);
        p.setPolicyholderId("C000000001");
        p.setBranchCode(branchCode);
        p.setTotalPremium(BigDecimal.ZERO);
        p.setLastUpdated(LocalDateTime.now());
        p.setUpdatedBy("SYSTEM");
        return p;
    }

    private PolicyHolder buildHolder(int creditScore, String riskTier) {
        PolicyHolder h = new PolicyHolder();
        h.setCustId("C000000001");
        h.setCreditScore(creditScore);
        h.setRiskTier(riskTier);
        h.setCreatedDate(LocalDate.now());
        h.setLastUpdated(LocalDateTime.now());
        return h;
    }

    @Test
    void evaluateRisk_policyNotFound_throws() {
        when(policyRepository.findById("NOTFOUND")).thenReturn(Optional.empty());
        assertThrows(PolicyNotFoundException.class,
                () -> service.evaluateRisk("NOTFOUND"));
    }

    @Test
    void evaluateRisk_score299_autoAccept() {
        // creditScore=750 → base = (900-750)/2 = 75
        // Standard tier → no adjustment
        // coverageLimit < 5M → no adjustment
        // No accumulation → no adjustment
        // Total = 75 → AP
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(750, "S");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertTrue(result.getRiskScore() < 300);
        assertEquals("AP", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_score300_referSenior() {
        // Need score exactly 300
        // creditScore=300 → base = (900-300)/2 = 300
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(300, "S");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertEquals(300, result.getRiskScore());
        assertEquals("RS", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_score599_stillReferSenior() {
        // creditScore=0 → base = 400
        // riskTier=U → +200 = 600
        // That's 600, not 599. Let's use creditScore=2 → base = (900-2)/2 = 449
        // riskTier=S → no adj
        // coverageLimit > 5M → +100 → 549
        // coverageLimit < 10M → no additional
        // Total = 549 → RS
        Policy p = buildPolicy(new BigDecimal("6000000"), "CHI1");
        PolicyHolder h = buildHolder(2, "S");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertTrue(result.getRiskScore() >= 300 && result.getRiskScore() < 600);
        assertEquals("RS", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_score600_referManager() {
        // creditScore=0 → base=400, riskTier=U → +200 = 600
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(0, "U");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertEquals(600, result.getRiskScore());
        assertEquals("RM", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_score799_stillReferManager() {
        // creditScore=0 → base=400, riskTier=U → +200=600, coverageLimit>5M → +100=700
        // Still < 800 → RM
        Policy p = buildPolicy(new BigDecimal("6000000"), "CHI1");
        PolicyHolder h = buildHolder(0, "U");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertTrue(result.getRiskScore() >= 600 && result.getRiskScore() < 800);
        assertEquals("RM", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_score800Plus_autoDecline() {
        // creditScore=0 → base=400, riskTier=U → +200=600, coverageLimit>10M → +100+200=900
        Policy p = buildPolicy(new BigDecimal("11000000"), "CHI1");
        PolicyHolder h = buildHolder(0, "U");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertTrue(result.getRiskScore() >= 800);
        assertEquals("DC", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_preferredTier_reducesScore() {
        // creditScore=750 → base=75, riskTier=P → -100 → max(0, -25) = 0 → AP
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(750, "P");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertEquals(0, result.getRiskScore());
        assertEquals("AP", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_accumulationOver500M_adds300() {
        // creditScore=750 → base=75, no tier adj, no limit adj
        // Accumulation > 500M → +300 → 375 → RS
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(750, "S");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus("CHI1", "AC"))
                .thenReturn(new BigDecimal("600000000"));
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UnderwritingResult result = service.evaluateRisk(p.getPolicyNumber());

        assertEquals(375, result.getRiskScore());
        assertEquals("RS", result.getDecisionCode());
    }

    @Test
    void evaluateRisk_jmsFailure_doesNotThrow() {
        Policy p = buildPolicy(new BigDecimal("100000"), "CHI1");
        PolicyHolder h = buildHolder(750, "S");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyHolderRepository.findById("C000000001")).thenReturn(Optional.of(h));
        when(policyRepository.sumCoverageLimitByBranchCodeAndPolicyStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(uwDecisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("MQ down")).when(jmsTemplate)
                .convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> service.evaluateRisk(p.getPolicyNumber()));
    }
}
