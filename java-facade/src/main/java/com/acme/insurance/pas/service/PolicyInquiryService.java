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
 * Mirrors POLQRY.cbl paragraphs 1000 through 5000.
 *
 * @author T. Nguyen (2022)
 */
@Service
public class PolicyInquiryService {

    @Autowired
    private PolicyRepository policyRepository;

    public PolicyInquiryResponse inquire(String policyNumber) {
        // Step 1: Validate input (mirrors 1000-RECEIVE-INPUT)
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY NUMBER IS REQUIRED");
        }

        // Step 2: Read policy (mirrors 2000-READ-POLICY)
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null; // POLICY NOT FOUND
        }

        // Step 3: Read customer (mirrors 3000-READ-CUSTOMER)
        Customer customer = policyRepository.findCustomerById(policy.getPolicyholderId());

        // Step 4: Read coverages (mirrors 4000-READ-COVERAGES, max 20)
        List<Coverage> coverages = policyRepository.findCoveragesByPolicyNumber(policyNumber);
        if (coverages.size() > 20) {
            coverages = coverages.subList(0, 20);
        }

        // Step 5: Build response (mirrors 5000-DISPLAY-POLICY)
        PolicyInquiryResponse response = new PolicyInquiryResponse();
        response.setPolicy(policy);
        response.setCustomer(customer);
        response.setCoverages(coverages);
        return response;
    }
}
