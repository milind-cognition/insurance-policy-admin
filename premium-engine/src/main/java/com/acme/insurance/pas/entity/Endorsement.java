package com.acme.insurance.pas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from DB2 table ACMEINS.ENDORSEMENTS.
 * Records mid-term policy modifications.
 */
@Entity
@Table(name = "ENDORSEMENTS", schema = "ACMEINS")
@IdClass(EndorsementId.class)
public class Endorsement {

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Id
    @Column(name = "ENDORSEMENT_SEQ", nullable = false)
    private Integer endorsementSeq;

    @Column(name = "ENDORSEMENT_TYPE", length = 3, nullable = false)
    private String endorsementType;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "PREMIUM_ADJUSTMENT", precision = 11, scale = 2)
    private BigDecimal premiumAdjustment;

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

    public Integer getEndorsementSeq() {
        return endorsementSeq;
    }

    public void setEndorsementSeq(Integer endorsementSeq) {
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
