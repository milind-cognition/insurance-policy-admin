package com.acme.insurance.pas.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Endorsement domain model.
 * Maps to ACMEINS.ENDORSEMENTS DB2 table on the mainframe.
 */
public class Endorsement {

    private String policyNumber;
    private int endorsementSeq;
    private String endorsementType;
    private Date effectiveDate;
    private String description;
    private BigDecimal premiumAdjustment;
    private Date processedDate;
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

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
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

    public Date getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(Date processedDate) {
        this.processedDate = processedDate;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
}
