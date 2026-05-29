package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Customer;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.PolicyInquiryResponse;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Policy Inquiry Service - Replicates the POLQRY COBOL paragraph flow.
 *
 * Paragraph mapping:
 *   1000-RECEIVE-INPUT  → validate policyNumber (blank/null check)
 *   2000-READ-POLICY    → findByPolicyNumber
 *   3000-READ-CUSTOMER  → findCustomerById(policyholderId)
 *   4000-READ-COVERAGES → findCoveragesByPolicyNumber, cap at 20
 *   5000-DISPLAY-POLICY → build PolicyInquiryResponse
 *
 * @author Devin (2026) - Migrated from POLQRY COBOL program
 */
@Service
public class PolicyInquiryService {

    private static final int MAX_COVERAGES = 20;

    @Autowired
    private PolicyRepository policyRepository;

    public PolicyInquiryResponse inquire(String policyNumber) {
        // 1000-RECEIVE-INPUT: validate input
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY NUMBER IS REQUIRED");
        }

        // 2000-READ-POLICY: look up policy
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null;
        }

        // 3000-READ-CUSTOMER: look up policyholder
        Customer customer = policyRepository.findCustomerById(policy.getPolicyholderId());

        // 4000-READ-COVERAGES: fetch coverages, cap at 20
        List<Coverage> coverages = policyRepository.findCoveragesByPolicyNumber(policyNumber);
        if (coverages.size() > MAX_COVERAGES) {
            coverages = coverages.subList(0, MAX_COVERAGES);
        }

        // 5000-DISPLAY-POLICY: build response
        PolicyInquiryResponse response = new PolicyInquiryResponse();
        response.setPolicy(policy);
        response.setCustomer(customer);
        response.setCoverages(coverages);
        return response;
    }
}
