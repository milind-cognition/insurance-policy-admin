package com.acme.insurance.pas.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps fields from COBOL copybook PREMIUM-RECORD.cpy and the DB2 PREMIUMS table.
 */
public class PremiumRecord {

    private String policyNumber;
    private int coverageSeq;
    private LocalDate termEffDate;
    private LocalDate termExpDate;
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
    private LocalDate calcDate;
    private String calcBy;

    public PremiumRecord() {
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

    public LocalDate getTermEffDate() {
        return termEffDate;
    }

    public void setTermEffDate(LocalDate termEffDate) {
        this.termEffDate = termEffDate;
    }

    public LocalDate getTermExpDate() {
        return termExpDate;
    }

    public void setTermExpDate(LocalDate termExpDate) {
        this.termExpDate = termExpDate;
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

    public LocalDate getCalcDate() {
        return calcDate;
    }

    public void setCalcDate(LocalDate calcDate) {
        this.calcDate = calcDate;
    }

    public String getCalcBy() {
        return calcBy;
    }

    public void setCalcBy(String calcBy) {
        this.calcBy = calcBy;
    }
}
