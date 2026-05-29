package com.acme.insurance.pas.model;

/**
 * Request DTO for policy renewal.
 * Mirrors input from CICS transaction PRWL (POLRNW program).
 */
public class RenewalRequest {

    private String policyNumber;

    public RenewalRequest() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
}
