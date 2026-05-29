package com.acme.insurance.pas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity mapped from COBOL copybook PREMIUM-RECORD.cpy
 * against DB2 table ACMEINS.PREMIUMS.
 *
 * All COMP-3 financial fields use BigDecimal to preserve the exact
 * precision of the original packed-decimal COBOL fields.
 */
@Entity
@Table(name = "PREMIUMS", schema = "ACMEINS")
@IdClass(PremiumId.class)
public class Premium {

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Id
    @Column(name = "COVERAGE_SEQ", nullable = false)
    private Integer coverageSeq;

    @Id
    @Column(name = "TERM_EFFECTIVE_DATE", nullable = false)
    private LocalDate termEffectiveDate;

    @Column(name = "TERM_EXPIRY_DATE", nullable = false)
    private LocalDate termExpiryDate;

    @Column(name = "BASE_RATE", precision = 11, scale = 4)
    private BigDecimal baseRate;

    @Column(name = "TERRITORY_FACTOR", precision = 7, scale = 4)
    private BigDecimal territoryFactor;

    @Column(name = "CLASS_FACTOR", precision = 7, scale = 4)
    private BigDecimal classFactor;

    @Column(name = "EXPERIENCE_MOD", precision = 7, scale = 4)
    private BigDecimal experienceMod;

    @Column(name = "SCHEDULE_MOD", precision = 7, scale = 4)
    private BigDecimal scheduleMod;

    @Column(name = "DISCOUNT_PCT", precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "SURCHARGE_AMT", precision = 9, scale = 2)
    private BigDecimal surchargeAmt;

    @Column(name = "TAX_AMT", precision = 9, scale = 2)
    private BigDecimal taxAmt;

    @Column(name = "TOTAL_PREMIUM", precision = 11, scale = 2)
    private BigDecimal totalPremium;

    @Column(name = "INSTALLMENT_CODE", length = 2)
    private String installmentCode;

    @Column(name = "INSTALLMENT_AMT", precision = 9, scale = 2)
    private BigDecimal installmentAmt;

    @Column(name = "CALC_DATE")
    private LocalDate calcDate;

    @Column(name = "CALC_BY", length = 8)
    private String calcBy;

    public Premium() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Integer getCoverageSeq() {
        return coverageSeq;
    }

    public void setCoverageSeq(Integer coverageSeq) {
        this.coverageSeq = coverageSeq;
    }

    public LocalDate getTermEffectiveDate() {
        return termEffectiveDate;
    }

    public void setTermEffectiveDate(LocalDate termEffectiveDate) {
        this.termEffectiveDate = termEffectiveDate;
    }

    public LocalDate getTermExpiryDate() {
        return termExpiryDate;
    }

    public void setTermExpiryDate(LocalDate termExpiryDate) {
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
