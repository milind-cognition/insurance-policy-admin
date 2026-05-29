package com.acme.insurance.pas.dto;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.PolicyHolder;

import java.util.List;

public class PolicyDetailResponse {

    private Policy policy;
    private PolicyHolder policyHolder;
    private List<Coverage> coverages;

    public PolicyDetailResponse() {
    }

    public PolicyDetailResponse(Policy policy, PolicyHolder policyHolder, List<Coverage> coverages) {
        this.policy = policy;
        this.policyHolder = policyHolder;
        this.coverages = coverages;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public PolicyHolder getPolicyHolder() {
        return policyHolder;
    }

    public void setPolicyHolder(PolicyHolder policyHolder) {
        this.policyHolder = policyHolder;
    }

    public List<Coverage> getCoverages() {
        return coverages;
    }

    public void setCoverages(List<Coverage> coverages) {
        this.coverages = coverages;
    }
}
