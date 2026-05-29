package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UNDERWRITING_DECISIONS", schema = "ACMEINS")
@IdClass(UnderwritingDecisionId.class)
public class UnderwritingDecision {

    public static final String DECISION_ACCEPT = "AP";
    public static final String DECISION_REFER_SENIOR = "RS";
    public static final String DECISION_REFER_MANAGER = "RM";
    public static final String DECISION_DECLINE = "DC";

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Id
    @Column(name = "DECISION_DATE", nullable = false)
    private LocalDate decisionDate;

    @Column(name = "DECISION_CODE", length = 2, nullable = false)
    private String decisionCode;

    @Column(name = "RISK_SCORE")
    private int riskScore;

    @Column(name = "DECISION_REASON", length = 100)
    private String decisionReason;

    @Column(name = "UNDERWRITER_ID", length = 8)
    private String underwriterId;

    @Column(name = "OVERRIDE_REASON", length = 200)
    private String overrideReason;

    @Column(name = "OVERRIDE_BY", length = 8)
    private String overrideBy;

    @Column(name = "CREATED_TIMESTAMP", nullable = false)
    private LocalDateTime createdTimestamp;

    public UnderwritingDecision() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public LocalDate getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDate decisionDate) {
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

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
