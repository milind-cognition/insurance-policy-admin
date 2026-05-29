package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * Endorsement request DTO.
 * Maps to the CICS COMMAREA input for POLEND transaction (PEND).
 */
public class EndorsementRequest {

    private String policyNumber;
    private String endorsementType;
    private BigDecimal premiumAdjustment;
    private String description;

    public EndorsementRequest() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getEndorsementType() {
        return endorsementType;
    }

    public void setEndorsementType(String endorsementType) {
        this.endorsementType = endorsementType;
    }

    public BigDecimal getPremiumAdjustment() {
        return premiumAdjustment;
    }

    public void setPremiumAdjustment(BigDecimal premiumAdjustment) {
        this.premiumAdjustment = premiumAdjustment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
