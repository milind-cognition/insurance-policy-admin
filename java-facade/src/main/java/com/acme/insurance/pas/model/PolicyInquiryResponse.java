package com.acme.insurance.pas.model;

import java.util.List;

/**
 * Combined policy inquiry response DTO.
 * Represents the output of the COBOL POLQRY program — all the data
 * collected before calling 5000-DISPLAY-POLICY.
 *
 * @author Devin (2026) - migrated from COBOL POLQRY
 */
public class PolicyInquiryResponse {

    private Policy policy;
    private Customer customer;
    private List<Coverage> coverages;

    public PolicyInquiryResponse() {
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Coverage> getCoverages() {
        return coverages;
    }

    public void setCoverages(List<Coverage> coverages) {
        this.coverages = coverages;
    }
}
