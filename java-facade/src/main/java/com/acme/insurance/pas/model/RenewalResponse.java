package com.acme.insurance.pas.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Response DTO for policy renewal.
 * Mirrors the confirmation data sent by COBOL paragraph 8000-SEND-CONFIRMATION.
 */
public class RenewalResponse {

    private String policyNumber;
    private BigDecimal previousPremium;
    private BigDecimal newPremium;
    private BigDecimal rateChangePct;
    private boolean rateCapped;
    private Date newEffectiveDate;
    private Date newExpiryDate;
    private int renewalCount;

    public RenewalResponse() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public BigDecimal getPreviousPremium() {
        return previousPremium;
    }

    public void setPreviousPremium(BigDecimal previousPremium) {
        this.previousPremium = previousPremium;
    }

    public BigDecimal getNewPremium() {
        return newPremium;
    }

    public void setNewPremium(BigDecimal newPremium) {
        this.newPremium = newPremium;
    }

    public BigDecimal getRateChangePct() {
        return rateChangePct;
    }

    public void setRateChangePct(BigDecimal rateChangePct) {
        this.rateChangePct = rateChangePct;
    }

    public boolean isRateCapped() {
        return rateCapped;
    }

    public void setRateCapped(boolean rateCapped) {
        this.rateCapped = rateCapped;
    }

    public Date getNewEffectiveDate() {
        return newEffectiveDate;
    }

    public void setNewEffectiveDate(Date newEffectiveDate) {
        this.newEffectiveDate = newEffectiveDate;
    }

    public Date getNewExpiryDate() {
        return newExpiryDate;
    }

    public void setNewExpiryDate(Date newExpiryDate) {
        this.newExpiryDate = newExpiryDate;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }
}
