package com.acme.insurance.pas.model;

/**
 * Request DTO for underwriting risk evaluation.
 * Mirrors the CICS RECEIVE map data from UNDWRT.cbl.
 */
public class UnderwritingRequest {

    private String policyNumber;

    public UnderwritingRequest() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
}
