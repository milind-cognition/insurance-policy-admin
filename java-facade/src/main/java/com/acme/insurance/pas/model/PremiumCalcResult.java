package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * Per-policy result DTO for premium batch calculation.
 */
public class PremiumCalcResult {

    private String policyNumber;
    private String policyType;
    private BigDecimal basePremium;
    private BigDecimal taxAmount;
    private BigDecimal surchargeAmount;
    private BigDecimal totalPremium;
    private String status;

    public PremiumCalcResult() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getSurchargeAmount() {
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
