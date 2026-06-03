package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new insurance policy.
 * Maps to the input fields from COBOL program POLNEW (BMS map).
 */
public class NewPolicyRequest {

    private String policyholderId;
    private String policyType;
    private String effectiveDate;
    private String expiryDate;
    private String agentCode;
    private String branchCode;
    private BigDecimal totalPremium;
    private BigDecimal deductible;
    private BigDecimal coverageLimit;

    public NewPolicyRequest() {
    }

    public String getPolicyholderId() {
        return policyholderId;
    }

    public void setPolicyholderId(String policyholderId) {
        this.policyholderId = policyholderId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }
}
