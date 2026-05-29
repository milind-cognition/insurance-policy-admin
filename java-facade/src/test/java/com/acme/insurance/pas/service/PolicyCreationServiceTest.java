package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.PolicyCreationRequest;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyCreationServiceTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private CoverageRepository coverageRepository;
    @Mock
    private PolicyHolderRepository policyHolderRepository;
    @Mock
    private JmsTemplate jmsTemplate;

    private PolicyCreationService service;

    @BeforeEach
    void setUp() {
        service = new PolicyCreationService(policyRepository, coverageRepository,
                policyHolderRepository, jmsTemplate);
    }

    private PolicyCreationRequest buildRequest(String type) {
        PolicyCreationRequest req = new PolicyCreationRequest();
        req.setPolicyType(type);
        req.setEffectiveDate(LocalDate.now().plusDays(1));
        req.setPolicyholderId("C000000001");
        req.setCoverageLimit(new BigDecimal("100000.00"));
        req.setBranchCode("CHI1");
        return req;
    }

    @Test
    void createPolicy_blankPolicyholderId_throws() {
        PolicyCreationRequest req = buildRequest("AUT");
        req.setPolicyholderId("");
        assertThrows(PolicyValidationException.class, () -> service.createPolicy(req));
    }

    @Test
    void createPolicy_blankPolicyType_throws() {
        PolicyCreationRequest req = buildRequest("");
        req.setPolicyType("");
        assertThrows(PolicyValidationException.class, () -> service.createPolicy(req));
    }

    @Test
    void createPolicy_pastEffectiveDate_throws() {
        PolicyCreationRequest req = buildRequest("AUT");
        req.setEffectiveDate(LocalDate.now().minusDays(1));
        assertThrows(PolicyValidationException.class, () -> service.createPolicy(req));
    }

    @Test
    void createPolicy_zeroCoverageLimit_throws() {
        PolicyCreationRequest req = buildRequest("AUT");
        req.setCoverageLimit(BigDecimal.ZERO);
        assertThrows(PolicyValidationException.class, () -> service.createPolicy(req));
    }

    @Test
    void createPolicy_holderNotFound_throws() {
        PolicyCreationRequest req = buildRequest("AUT");
        when(policyHolderRepository.existsById("C000000001")).thenReturn(false);
        assertThrows(PolicyValidationException.class, () -> service.createPolicy(req));
    }

    @Test
    void createPolicy_autoPolicy_generatesTwoCoverages() {
        PolicyCreationRequest req = buildRequest("AUT");
        when(policyHolderRepository.existsById(any())).thenReturn(true);
        when(policyRepository.getNextPolicySequence()).thenReturn(1000000L);
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.createPolicy(req);

        assertEquals("POL001000000", result.getPolicyNumber());
        assertEquals("PN", result.getPolicyStatus());

        ArgumentCaptor<Coverage> captor = ArgumentCaptor.forClass(Coverage.class);
        verify(coverageRepository, times(2)).save(captor.capture());
        List<Coverage> coverages = captor.getAllValues();
        assertEquals("AUTL", coverages.get(0).getCoverageType());
        assertEquals("AUTP", coverages.get(1).getCoverageType());
    }

    @Test
    void createPolicy_homePolicy_generatesTwoCoverages() {
        PolicyCreationRequest req = buildRequest("HOM");
        when(policyHolderRepository.existsById(any())).thenReturn(true);
        when(policyRepository.getNextPolicySequence()).thenReturn(1000001L);
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createPolicy(req);

        ArgumentCaptor<Coverage> captor = ArgumentCaptor.forClass(Coverage.class);
        verify(coverageRepository, times(2)).save(captor.capture());
        List<Coverage> coverages = captor.getAllValues();
        assertEquals("PROP", coverages.get(0).getCoverageType());
        assertEquals("LIAB", coverages.get(1).getCoverageType());
    }

    @Test
    void createPolicy_commercialPolicy_generatesThreeCoverages() {
        PolicyCreationRequest req = buildRequest("COM");
        when(policyHolderRepository.existsById(any())).thenReturn(true);
        when(policyRepository.getNextPolicySequence()).thenReturn(1000002L);
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createPolicy(req);

        ArgumentCaptor<Coverage> captor = ArgumentCaptor.forClass(Coverage.class);
        verify(coverageRepository, times(3)).save(captor.capture());
        List<Coverage> coverages = captor.getAllValues();
        assertEquals("PROP", coverages.get(0).getCoverageType());
        assertEquals("LIAB", coverages.get(1).getCoverageType());
        assertEquals("WKCP", coverages.get(2).getCoverageType());
    }

    @Test
    void createPolicy_jmsFailure_doesNotThrow() {
        PolicyCreationRequest req = buildRequest("AUT");
        when(policyHolderRepository.existsById(any())).thenReturn(true);
        when(policyRepository.getNextPolicySequence()).thenReturn(1000003L);
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("MQ down")).when(jmsTemplate)
                .convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> service.createPolicy(req));
    }

    @Test
    void createPolicy_policyNumberFormat() {
        PolicyCreationRequest req = buildRequest("AUT");
        when(policyHolderRepository.existsById(any())).thenReturn(true);
        when(policyRepository.getNextPolicySequence()).thenReturn(42L);
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.createPolicy(req);
        assertEquals("POL000000042", result.getPolicyNumber());
    }
}
