package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * DTO for incoming endorsement requests.
 * Corresponds to the BMS RECEIVE MAP in POLEND paragraph 2000.
 */
public class EndorsementRequest {

    private String endorsementType;
    private String description;
    private BigDecimal premiumAdjustment;

    public EndorsementRequest() {
    }

    public String getEndorsementType() {
        return endorsementType;
    }

    public void setEndorsementType(String endorsementType) {
        this.endorsementType = endorsementType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPremiumAdjustment() {
        return premiumAdjustment;
    }

    public void setPremiumAdjustment(BigDecimal premiumAdjustment) {
        this.premiumAdjustment = premiumAdjustment;
    }
}
