package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "POLICIES", schema = "ACMEINS")
public class Policy {

    public static final String STATUS_ACTIVE = "AC";
    public static final String STATUS_PENDING = "PN";
    public static final String STATUS_CANCELLED = "CN";
    public static final String STATUS_EXPIRED = "EX";
    public static final String STATUS_LAPSED = "LP";

    public static final String TYPE_AUTO = "AUT";
    public static final String TYPE_HOME = "HOM";
    public static final String TYPE_COMMERCIAL = "COM";
    public static final String TYPE_LIFE = "LIF";
    public static final String TYPE_HEALTH = "HLT";

    public static final String UW_PENDING = "PN";
    public static final String UW_AUTO_ACCEPT = "AP";
    public static final String UW_REFER_SENIOR = "RS";
    public static final String UW_REFER_MANAGER = "RM";
    public static final String UW_DECLINE = "DC";

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Column(name = "POLICY_TYPE", length = 3, nullable = false)
    private String policyType;

    @Column(name = "POLICY_STATUS", length = 2, nullable = false)
    private String policyStatus = STATUS_PENDING;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "POLICYHOLDER_ID", length = 10, nullable = false)
    private String policyholderId;

    @Column(name = "AGENT_CODE", length = 6)
    private String agentCode;

    @Column(name = "BRANCH_CODE", length = 4)
    private String branchCode;

    @Column(name = "TOTAL_PREMIUM", precision = 11, scale = 2)
    private BigDecimal totalPremium = BigDecimal.ZERO;

    @Column(name = "DEDUCTIBLE", precision = 9, scale = 2)
    private BigDecimal deductible = BigDecimal.ZERO;

    @Column(name = "COVERAGE_LIMIT", precision = 13, scale = 2)
    private BigDecimal coverageLimit = BigDecimal.ZERO;

    @Column(name = "INCEPTION_DATE")
    private LocalDate inceptionDate;

    @Column(name = "RENEWAL_COUNT")
    private int renewalCount = 0;

    @Column(name = "UW_STATUS", length = 2)
    private String uwStatus = UW_PENDING;

    @Column(name = "RISK_SCORE")
    private int riskScore = 0;

    @Column(name = "WEB_INDICATOR", length = 1)
    private String webIndicator = "N";

    @Column(name = "API_FLAG", length = 1)
    private String apiFlag = "N";

    @Column(name = "LAST_UPDATED", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "UPDATED_BY", length = 8, nullable = false)
    private String updatedBy = "SYSTEM";

    public Policy() {
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

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPolicyholderId() {
        return policyholderId;
    }

    public void setPolicyholderId(String policyholderId) {
        this.policyholderId = policyholderId;
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

    public LocalDate getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDate inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }

    public String getUwStatus() {
        return uwStatus;
    }

    public void setUwStatus(String uwStatus) {
        this.uwStatus = uwStatus;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getWebIndicator() {
        return webIndicator;
    }

    public void setWebIndicator(String webIndicator) {
        this.webIndicator = webIndicator;
    }

    public String getApiFlag() {
        return apiFlag;
    }

    public void setApiFlag(String apiFlag) {
        this.apiFlag = apiFlag;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
