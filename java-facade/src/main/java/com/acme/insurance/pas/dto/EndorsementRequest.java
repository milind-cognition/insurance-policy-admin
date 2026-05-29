package com.acme.insurance.pas.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class EndorsementRequest {

    @NotBlank
    private String policyNumber;

    @NotBlank
    private String endorsementType;

    private BigDecimal premiumAdjustment;
    private String description;

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
