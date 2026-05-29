package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * DTO for endorsement processing results.
 * Corresponds to the BMS SEND MAP in POLEND paragraph 7000.
 */
public class EndorsementResponse {

    private String policyNumber;
    private int endorsementSeq;
    private String endorsementType;
    private BigDecimal premiumAdjustment;
    private BigDecimal prorataFactor;
    private BigDecimal newTotalPremium;
    private String message;

    public EndorsementResponse() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public int getEndorsementSeq() {
        return endorsementSeq;
    }

    public void setEndorsementSeq(int endorsementSeq) {
        this.endorsementSeq = endorsementSeq;
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

    public BigDecimal getProrataFactor() {
        return prorataFactor;
    }

    public void setProrataFactor(BigDecimal prorataFactor) {
        this.prorataFactor = prorataFactor;
    }

    public BigDecimal getNewTotalPremium() {
        return newTotalPremium;
    }

    public void setNewTotalPremium(BigDecimal newTotalPremium) {
        this.newTotalPremium = newTotalPremium;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
