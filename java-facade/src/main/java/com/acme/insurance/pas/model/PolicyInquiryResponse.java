package com.acme.insurance.pas.model;

import java.util.List;

/**
 * Combined response DTO for the Policy Inquiry endpoint.
 * Mirrors the POLQRY COBOL program's BMS output: policy header,
 * customer (policyholder) details, and coverage table.
 *
 * @author Devin (2026) - Migrated from POLQRY COBOL program
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
