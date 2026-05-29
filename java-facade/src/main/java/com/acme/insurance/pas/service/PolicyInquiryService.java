package com.acme.insurance.pas.service;

import com.acme.insurance.pas.dto.PolicyDetailResponse;
import com.acme.insurance.pas.exception.PolicyNotFoundException;
import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.PolicyHolder;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PolicyInquiryService {

    private static final int MAX_COVERAGES = 20;

    private final PolicyRepository policyRepository;
    private final PolicyHolderRepository policyHolderRepository;
    private final CoverageRepository coverageRepository;

    public PolicyInquiryService(PolicyRepository policyRepository,
                                PolicyHolderRepository policyHolderRepository,
                                CoverageRepository coverageRepository) {
        this.policyRepository = policyRepository;
        this.policyHolderRepository = policyHolderRepository;
        this.coverageRepository = coverageRepository;
    }

    @Transactional(readOnly = true)
    public PolicyDetailResponse inquirePolicy(String policyNumber) {
        Policy policy = policyRepository.findById(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

        PolicyHolder holder = policyHolderRepository.findById(policy.getPolicyholderId())
                .orElse(null);

        List<Coverage> coverages = coverageRepository
                .findByPolicyNumberOrderBySequenceNum(policyNumber);
        if (coverages.size() > MAX_COVERAGES) {
            coverages = coverages.subList(0, MAX_COVERAGES);
        }

        return new PolicyDetailResponse(policy, holder, coverages);
    }
}
