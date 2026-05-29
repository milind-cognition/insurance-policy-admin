package com.acme.insurance.pas.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Premium domain model.
 * Maps to ACMEINS.PREMIUMS DB2 table on the mainframe.
 */
public class Premium {

    private String policyNumber;
    private int coverageSeq;
    private Date termEffectiveDate;
    private Date termExpiryDate;
    private BigDecimal baseRate;
    private BigDecimal territoryFactor;
    private BigDecimal classFactor;
    private BigDecimal experienceMod;
    private BigDecimal scheduleMod;
    private BigDecimal discountPct;
    private BigDecimal surchargeAmt;
    private BigDecimal taxAmt;
    private BigDecimal totalPremium;
    private String installmentCode;
    private BigDecimal installmentAmt;
    private Date calcDate;
    private String calcBy;

    public Premium() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public int getCoverageSeq() {
        return coverageSeq;
    }

    public void setCoverageSeq(int coverageSeq) {
        this.coverageSeq = coverageSeq;
    }

    public Date getTermEffectiveDate() {
        return termEffectiveDate;
    }

    public void setTermEffectiveDate(Date termEffectiveDate) {
        this.termEffectiveDate = termEffectiveDate;
    }

    public Date getTermExpiryDate() {
        return termExpiryDate;
    }

    public void setTermExpiryDate(Date termExpiryDate) {
        this.termExpiryDate = termExpiryDate;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }

    public BigDecimal getTerritoryFactor() {
        return territoryFactor;
    }

    public void setTerritoryFactor(BigDecimal territoryFactor) {
        this.territoryFactor = territoryFactor;
    }

    public BigDecimal getClassFactor() {
        return classFactor;
    }

    public void setClassFactor(BigDecimal classFactor) {
        this.classFactor = classFactor;
    }

    public BigDecimal getExperienceMod() {
        return experienceMod;
    }

    public void setExperienceMod(BigDecimal experienceMod) {
        this.experienceMod = experienceMod;
    }

    public BigDecimal getScheduleMod() {
        return scheduleMod;
    }

    public void setScheduleMod(BigDecimal scheduleMod) {
        this.scheduleMod = scheduleMod;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct;
    }

    public BigDecimal getSurchargeAmt() {
        return surchargeAmt;
    }

    public void setSurchargeAmt(BigDecimal surchargeAmt) {
        this.surchargeAmt = surchargeAmt;
    }

    public BigDecimal getTaxAmt() {
        return taxAmt;
    }

    public void setTaxAmt(BigDecimal taxAmt) {
        this.taxAmt = taxAmt;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public String getInstallmentCode() {
        return installmentCode;
    }

    public void setInstallmentCode(String installmentCode) {
        this.installmentCode = installmentCode;
    }

    public BigDecimal getInstallmentAmt() {
        return installmentAmt;
    }

    public void setInstallmentAmt(BigDecimal installmentAmt) {
        this.installmentAmt = installmentAmt;
    }

    public Date getCalcDate() {
        return calcDate;
    }

    public void setCalcDate(Date calcDate) {
        this.calcDate = calcDate;
    }

    public String getCalcBy() {
        return calcBy;
    }

    public void setCalcBy(String calcBy) {
        this.calcBy = calcBy;
    }
}
