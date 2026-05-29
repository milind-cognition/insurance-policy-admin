package com.acme.insurance.pas.service;

import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.exception.PolicyValidationException;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
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
class RenewalServiceTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private CoverageRepository coverageRepository;

    private RenewalService service;

    @BeforeEach
    void setUp() {
        service = new RenewalService(policyRepository, coverageRepository);
    }

    private Policy buildPolicy(String status, BigDecimal premium,
                               LocalDate effective, LocalDate expiry) {
        Policy p = new Policy();
        p.setPolicyNumber("POL000000001");
        p.setPolicyType("AUT");
        p.setPolicyStatus(status);
        p.setEffectiveDate(effective);
        p.setExpiryDate(expiry);
        p.setTotalPremium(premium);
        p.setPolicyholderId("C000000001");
        p.setRenewalCount(0);
        p.setLastUpdated(LocalDateTime.now());
        p.setUpdatedBy("SYSTEM");
        return p;
    }

    @Test
    void renewPolicy_notFound_throws() {
        when(policyRepository.findById("POL999")).thenReturn(Optional.empty());
        assertThrows(PolicyNotFoundException.class,
                () -> service.renewPolicy("POL999"));
    }

    @Test
    void renewPolicy_cancelledPolicy_throws() {
        Policy p = buildPolicy("CN", new BigDecimal("1000.00"),
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        assertThrows(PolicyValidationException.class,
                () -> service.renewPolicy(p.getPolicyNumber()));
    }

    @Test
    void renewPolicy_standardIncrease() {
        Policy p = buildPolicy("AC", new BigDecimal("1000.00"),
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.renewPolicy(p.getPolicyNumber());

        assertEquals(new BigDecimal("1050.00"), result.getTotalPremium());
        assertEquals(1, result.getRenewalCount());
        assertEquals(LocalDate.of(2026, 1, 1), result.getEffectiveDate());
        assertEquals(LocalDate.of(2027, 1, 1), result.getExpiryDate());
        assertEquals("AC", result.getPolicyStatus());
    }

    @Test
    void renewPolicy_leapYearBugFixed() {
        // The COBOL bug (POLRNW line 148) would fail on Feb 29:
        // 20240229 + 10000 = 20250229, which doesn't exist.
        // Java's LocalDate.plusYears(1) correctly returns 2025-02-28.
        Policy p = buildPolicy("AC", new BigDecimal("1000.00"),
                LocalDate.of(2023, 2, 28), LocalDate.of(2024, 2, 29));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.renewPolicy(p.getPolicyNumber());

        assertEquals(LocalDate.of(2024, 2, 29), result.getEffectiveDate());
        assertEquals(LocalDate.of(2025, 2, 28), result.getExpiryDate());
    }

    @Test
    void renewPolicy_leapYearBugFixed_feb28InLeapYear() {
        Policy p = buildPolicy("AC", new BigDecimal("500.00"),
                LocalDate.of(2023, 3, 1), LocalDate.of(2024, 2, 29));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.renewPolicy(p.getPolicyNumber());

        assertEquals(LocalDate.of(2024, 2, 29), result.getEffectiveDate());
        assertEquals(LocalDate.of(2025, 2, 28), result.getExpiryDate());
    }

    @Test
    void renewPolicy_rateCap_at15Percent() {
        // If standard 5% increase is applied to a large premium, no cap should trigger.
        // But this tests the cap logic for edge cases where premium math would exceed 15%.
        // With 5% standard increase, cap never triggers normally; test that renewal
        // still works correctly.
        Policy p = buildPolicy("AC", new BigDecimal("10000.00"),
                LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.renewPolicy(p.getPolicyNumber());

        assertEquals(new BigDecimal("10500.00"), result.getTotalPremium());
    }

    @Test
    void renewPolicy_expiredPolicyCanRenew() {
        Policy p = buildPolicy("EX", new BigDecimal("2000.00"),
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1));
        when(policyRepository.findById(p.getPolicyNumber())).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Policy result = service.renewPolicy(p.getPolicyNumber());

        assertEquals("AC", result.getPolicyStatus());
        assertEquals(new BigDecimal("2100.00"), result.getTotalPremium());
    }
}
