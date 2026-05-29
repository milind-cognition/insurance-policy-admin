package com.acme.insurance.pas.model;

import java.util.List;

/**
 * Combined policy inquiry response DTO.
 * Mirrors the COBOL POLQRY program output which assembles
 * policy header + customer info + coverages (max 20) into
 * a single combined view for display on the BMS map.
 *
 * @author Devin (2026) - Migrated from COBOL POLQRY paragraph 5000-DISPLAY-POLICY
 */
public class PolicyInquiryResponse {

    private Policy policy;
    private Customer customer;
    private List<Coverage> coverages;
    private int coverageCount;

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

    public int getCoverageCount() {
        return coverageCount;
    }

    public void setCoverageCount(int coverageCount) {
        this.coverageCount = coverageCount;
    }
}
