package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.PremiumBatchSummary;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.Premium;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.repository.PremiumRepository;
import com.acme.insurance.pas.repository.TerritoryFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumCalculationServiceTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private PremiumRepository premiumRepository;
    @Mock
    private TerritoryFactorRepository territoryFactorRepository;
    @Mock
    private CoverageRepository coverageRepository;

    private PremiumCalculationService service;

    @BeforeEach
    void setUp() {
        service = new PremiumCalculationService(policyRepository,
                premiumRepository, territoryFactorRepository, coverageRepository);
    }

    private void stubDefaultMocks() {
        when(territoryFactorRepository.findByEffectiveDateLessThanEqual(any()))
                .thenReturn(Collections.emptyList());
        when(coverageRepository.findByPolicyNumberOrderBySequenceNum(any()))
                .thenReturn(Collections.emptyList());
    }

    private Policy buildPolicy(String type) {
        Policy p = new Policy();
        p.setPolicyNumber("POL000000001");
        p.setPolicyType(type);
        p.setPolicyStatus("AC");
        p.setEffectiveDate(LocalDate.now().minusMonths(6));
        p.setExpiryDate(LocalDate.now().plusMonths(6));
        p.setTotalPremium(BigDecimal.ZERO);
        p.setPolicyholderId("C000000001");
        p.setLastUpdated(LocalDateTime.now());
        p.setUpdatedBy("SYSTEM");
        return p;
    }

    @Test
    void calculateAllPremiums_autoBaseRate850() {
        Policy auto = buildPolicy("AUT");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(auto));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PremiumBatchSummary summary = service.calculateAllPremiums();

        assertEquals(1, summary.getPoliciesRead());
        assertEquals(1, summary.getPoliciesUpdated());
        assertEquals(0, summary.getPoliciesError());

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        Premium prem = captor.getValue();
        assertEquals(new BigDecimal("850.00"), prem.getBaseRate());

        BigDecimal expectedTax = new BigDecimal("850.00")
                .multiply(new BigDecimal("0.0350"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, prem.getTaxAmt());

        BigDecimal expectedTotal = new BigDecimal("850.00")
                .add(expectedTax).add(new BigDecimal("25.00"));
        assertEquals(expectedTotal, prem.getTotalPremium());
    }

    @Test
    void calculateAllPremiums_homeBaseRate1200() {
        Policy home = buildPolicy("HOM");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(home));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        assertEquals(new BigDecimal("1200.00"), captor.getValue().getBaseRate());
    }

    @Test
    void calculateAllPremiums_commercialBaseRate5000() {
        Policy com = buildPolicy("COM");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(com));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        assertEquals(new BigDecimal("5000.00"), captor.getValue().getBaseRate());
    }

    @Test
    void calculateAllPremiums_lifeBaseRate400() {
        Policy life = buildPolicy("LIF");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(life));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        assertEquals(new BigDecimal("400.00"), captor.getValue().getBaseRate());
    }

    @Test
    void calculateAllPremiums_healthBaseRate3500() {
        Policy health = buildPolicy("HLT");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(health));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        assertEquals(new BigDecimal("3500.00"), captor.getValue().getBaseRate());
    }

    @Test
    void calculateAllPremiums_unknownType_defaultRate1000() {
        Policy unknown = buildPolicy("XYZ");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(unknown));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        assertEquals(new BigDecimal("1000.00"), captor.getValue().getBaseRate());
    }

    @Test
    void calculateAllPremiums_taxAndSurchargeCalculation() {
        Policy auto = buildPolicy("AUT");
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(List.of(auto));
        stubDefaultMocks();
        when(premiumRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.calculateAllPremiums();

        ArgumentCaptor<Premium> captor = ArgumentCaptor.forClass(Premium.class);
        verify(premiumRepository).save(captor.capture());
        Premium prem = captor.getValue();

        assertEquals(new BigDecimal("25.00"), prem.getSurchargeAmt());
        BigDecimal expectedTax = new BigDecimal("850.00")
                .multiply(new BigDecimal("0.0350"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTax, prem.getTaxAmt());
    }

    @Test
    void calculateAllPremiums_noPolicies_zeroCounters() {
        when(policyRepository.findByPolicyStatus("AC")).thenReturn(Collections.emptyList());
        when(territoryFactorRepository.findByEffectiveDateLessThanEqual(any()))
                .thenReturn(Collections.emptyList());

        PremiumBatchSummary summary = service.calculateAllPremiums();

        assertEquals(0, summary.getPoliciesRead());
        assertEquals(0, summary.getPoliciesUpdated());
        assertEquals(0, summary.getPoliciesError());
    }
}
