package com.acme.insurance.pas.model;

import java.math.BigDecimal;

/**
 * Response DTO for underwriting risk evaluation.
 * Mirrors the 8000-DISPLAY-RESULT output from UNDWRT.cbl.
 */
public class UnderwritingResponse {

    private String policyNumber;
    private int riskScore;
    private String decisionCode;
    private String decisionReason;
    private int claimCount;
    private BigDecimal lossRatio;
    private BigDecimal accumulatedLimit;

    public UnderwritingResponse() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecisionCode() {
        return decisionCode;
    }

    public void setDecisionCode(String decisionCode) {
        this.decisionCode = decisionCode;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public int getClaimCount() {
        return claimCount;
    }

    public void setClaimCount(int claimCount) {
        this.claimCount = claimCount;
    }

    public BigDecimal getLossRatio() {
        return lossRatio;
    }

    public void setLossRatio(BigDecimal lossRatio) {
        this.lossRatio = lossRatio;
    }

    public BigDecimal getAccumulatedLimit() {
        return accumulatedLimit;
    }

    public void setAccumulatedLimit(BigDecimal accumulatedLimit) {
        this.accumulatedLimit = accumulatedLimit;
    }
}
