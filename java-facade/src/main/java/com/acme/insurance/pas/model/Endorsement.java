package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ENDORSEMENTS", schema = "ACMEINS")
@IdClass(EndorsementId.class)
public class Endorsement {

    public static final String TYPE_COV_ADD = "CAD";
    public static final String TYPE_COV_REMOVE = "CRM";
    public static final String TYPE_LIMIT_CHANGE = "LCH";
    public static final String TYPE_ADDR_CHANGE = "ACH";
    public static final String TYPE_CANCEL = "CAN";

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Id
    @Column(name = "ENDORSEMENT_SEQ", nullable = false)
    private int endorsementSeq;

    @Column(name = "ENDORSEMENT_TYPE", length = 3, nullable = false)
    private String endorsementType;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "PREMIUM_ADJUSTMENT", precision = 11, scale = 2)
    private BigDecimal premiumAdjustment = BigDecimal.ZERO;

    @Column(name = "PROCESSED_DATE", nullable = false)
    private LocalDateTime processedDate;

    @Column(name = "PROCESSED_BY", length = 8, nullable = false)
    private String processedBy;

    public Endorsement() {
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

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPremiumAdjustment() {
        return premiumAdjustment;
    }

    public void setPremiumAdjustment(BigDecimal premiumAdjustment) {
        this.premiumAdjustment = premiumAdjustment;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
}
