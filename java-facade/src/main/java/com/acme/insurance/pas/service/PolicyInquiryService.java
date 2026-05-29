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
 * Policy Inquiry Service.
 * Mirrors the COBOL POLQRY program flow (paragraphs 1000-5000):
 *   1. Validate input  (1000-RECEIVE-INPUT)
 *   2. Read policy      (2000-READ-POLICY)
 *   3. Read customer    (3000-READ-CUSTOMER)
 *   4. Read coverages   (4000-READ-COVERAGES, max 20 cap)
 *   5. Assemble response(5000-DISPLAY-POLICY)
 *
 * @author Devin (2026) - Migrated from COBOL POLQRY
 */
@Service
public class PolicyInquiryService {

    private static final int MAX_COVERAGES = 20;

    @Autowired
    private PolicyRepository policyRepository;

    /**
     * Perform a full policy inquiry matching the COBOL POLQRY transaction.
     *
     * @param policyNumber the policy number to look up
     * @return combined inquiry response, or null if policy not found
     * @throws IllegalArgumentException if policyNumber is blank/null
     */
    public PolicyInquiryResponse inquirePolicy(String policyNumber) {
        // 1000-RECEIVE-INPUT: validate input
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("POLICY NUMBER IS REQUIRED");
        }

        // 2000-READ-POLICY: read policy header
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return null;
        }

        // 3000-READ-CUSTOMER: read customer record
        Customer customer = policyRepository.findCustomerById(policy.getPolicyholderId());

        // 4000-READ-COVERAGES: read coverages with max 20 cap
        List<Coverage> coverages = policyRepository.findCoveragesByPolicyNumber(policyNumber);
        if (coverages.size() > MAX_COVERAGES) {
            coverages = coverages.subList(0, Math.min(coverages.size(), MAX_COVERAGES));
        }

        // 5000-DISPLAY-POLICY: assemble combined response
        PolicyInquiryResponse response = new PolicyInquiryResponse();
        response.setPolicy(policy);
        response.setCustomer(customer);
        response.setCoverages(coverages);
        response.setCoverageCount(coverages.size());
        return response;
    }
}
