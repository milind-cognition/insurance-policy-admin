package com.acme.insurance.pas.model;

import java.util.Date;

/**
 * Underwriting Decision domain model.
 * Maps to ACMEINS.UNDERWRITING_DECISIONS DB2 table on the mainframe.
 */
public class UnderwritingDecision {

    private String policyNumber;
    private Date decisionDate;
    private String decisionCode;
    private int riskScore;
    private String decisionReason;
    private String underwriterId;
    private String overrideReason;
    private String overrideBy;
    private Date createdTimestamp;

    public UnderwritingDecision() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Date getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(Date decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getDecisionCode() {
        return decisionCode;
    }

    public void setDecisionCode(String decisionCode) {
        this.decisionCode = decisionCode;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(String underwriterId) {
        this.underwriterId = underwriterId;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getOverrideBy() {
        return overrideBy;
    }

    public void setOverrideBy(String overrideBy) {
        this.overrideBy = overrideBy;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
