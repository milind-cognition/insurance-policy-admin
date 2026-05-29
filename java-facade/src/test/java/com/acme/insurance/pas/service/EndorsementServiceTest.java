package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.EndorsementRequest;
import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.EndorsementRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndorsementServiceTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private EndorsementRepository endorsementRepository;
    @Mock
    private EntityManager entityManager;

    private EndorsementService service;

    @BeforeEach
    void setUp() {
        service = new EndorsementService(policyRepository, endorsementRepository, entityManager);
    }

    private Policy buildActivePolicy() {
        Policy p = new Policy();
        p.setPolicyNumber("POL000000001");
        p.setPolicyType("AUT");
        p.setPolicyStatus("AC");
        p.setEffectiveDate(LocalDate.now().minusMonths(6));
        p.setExpiryDate(LocalDate.now().plusMonths(6));
        p.setTotalPremium(new BigDecimal("1000.00"));
        p.setPolicyholderId("C000000001");
        p.setLastUpdated(LocalDateTime.now());
        p.setUpdatedBy("SYSTEM");
        return p;
    }

    @Test
    void processEndorsement_policyNotFound_throws() {
        EndorsementRequest req = new EndorsementRequest();
        req.setPolicyNumber("NOTFOUND");
        req.setEndorsementType("CAD");
        when(policyRepository.findById("NOTFOUND")).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class,
                () -> service.processEndorsement(req));
    }

    @Test
    void processEndorsement_notActivePolicy_throws() {
        Policy p = buildActivePolicy();
        p.setPolicyStatus("CN");
        EndorsementRequest req = new EndorsementRequest();
        req.setPolicyNumber(p.getPolicyNumber());
        req.setEndorsementType("CAD");
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));

        assertThrows(PolicyValidationException.class,
                () -> service.processEndorsement(req));
    }

    @Test
    void processEndorsement_blankType_throws() {
        Policy p = buildActivePolicy();
        EndorsementRequest req = new EndorsementRequest();
        req.setPolicyNumber(p.getPolicyNumber());
        req.setEndorsementType("");
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));

        assertThrows(PolicyValidationException.class,
                () -> service.processEndorsement(req));
    }

    @Test
    void processEndorsement_success_updatesPremium() {
        Policy p = buildActivePolicy();
        EndorsementRequest req = new EndorsementRequest();
        req.setPolicyNumber(p.getPolicyNumber());
        req.setEndorsementType("CAD");
        req.setPremiumAdjustment(new BigDecimal("200.00"));
        req.setDescription("Add coverage");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(endorsementRepository.findMaxEndorsementSeqByPolicyNumber(p.getPolicyNumber()))
                .thenReturn(0);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Endorsement result = service.processEndorsement(req);

        assertEquals(1, result.getEndorsementSeq());
        assertEquals("CAD", result.getEndorsementType());
        assertEquals(new BigDecimal("1200.00"), p.getTotalPremium());
    }

    @Test
    void processEndorsement_nullPremiumAdjustment_noChange() {
        Policy p = buildActivePolicy();
        EndorsementRequest req = new EndorsementRequest();
        req.setPolicyNumber(p.getPolicyNumber());
        req.setEndorsementType("ACH");

        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(endorsementRepository.findMaxEndorsementSeqByPolicyNumber(p.getPolicyNumber()))
                .thenReturn(2);
        when(endorsementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Endorsement result = service.processEndorsement(req);

        assertEquals(3, result.getEndorsementSeq());
        assertEquals(new BigDecimal("1000.00"), p.getTotalPremium());
    }
}
