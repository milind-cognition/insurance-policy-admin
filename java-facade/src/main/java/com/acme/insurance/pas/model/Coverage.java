package com.acme.insurance.pas.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Coverage domain model.
 * Maps to ACMEINS.COVERAGES DB2 table on the mainframe.
 *
 * @author T. Nguyen (2022)
 */
public class Coverage {

    private String policyNumber;
    private int sequenceNum;
    private String coverageType;
    private String description;
    private BigDecimal coverageLimit;
    private BigDecimal deductible;
    private BigDecimal premium;
    private Date effectiveDate;
    private Date expiryDate;
    private String status;
    private int coinsurancePct;
    private String ratingTerritory;
    private String classCode;

    public Coverage() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(int sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCoinsurancePct() {
        return coinsurancePct;
    }

    public void setCoinsurancePct(int coinsurancePct) {
        this.coinsurancePct = coinsurancePct;
    }

    public String getRatingTerritory() {
        return ratingTerritory;
    }

    public void setRatingTerritory(String ratingTerritory) {
        this.ratingTerritory = ratingTerritory;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
}
