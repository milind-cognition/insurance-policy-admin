package com.acme.insurance.pas.dto;

public class UnderwritingResult {

    private String policyNumber;
    private int riskScore;
    private String decisionCode;
    private String decisionReason;

    public UnderwritingResult() {
    }

    public UnderwritingResult(String policyNumber, int riskScore,
                              String decisionCode, String decisionReason) {
        this.policyNumber = policyNumber;
        this.riskScore = riskScore;
        this.decisionCode = decisionCode;
        this.decisionReason = decisionReason;
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
}
