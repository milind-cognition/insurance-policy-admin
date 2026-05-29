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
 * Policy Inquiry Service - replicates the COBOL POLQRY program logic.
 *
 * Mirrors the mainline flow of POLQRY.cbl (paragraphs 1000 through 5000):
 *   1000-RECEIVE-INPUT   → validate policy number
 *   2000-READ-POLICY     → fetch policy record
 *   3000-READ-CUSTOMER   → fetch policyholder record
 *   4000-READ-COVERAGES  → fetch coverage records (max 20)
 *   5000-DISPLAY-POLICY  → build combined response
 *
 * @author Devin (2026)
 */
@Service
public class PolicyInquiryService {

    @Autowired
    private PolicyRepository policyRepository;

    public PolicyInquiryResponse inquire(String policyNumber) {
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY NUMBER IS REQUIRED");
        }

        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null;
        }

        Customer customer = policyRepository.findCustomerById(policy.getPolicyholderId());

        List<Coverage> coverages = policyRepository.findCoveragesByPolicyNumber(policyNumber);
        if (coverages.size() > 20) {
            coverages = coverages.subList(0, 20);
        }

        PolicyInquiryResponse response = new PolicyInquiryResponse();
        response.setPolicy(policy);
        response.setCustomer(customer);
        response.setCoverages(coverages);
        return response;
    }
}
