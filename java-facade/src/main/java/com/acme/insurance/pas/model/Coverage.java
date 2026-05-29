package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "COVERAGES", schema = "ACMEINS")
@IdClass(CoverageId.class)
public class Coverage {

    public static final String TYPE_PROP = "PROP";
    public static final String TYPE_LIAB = "LIAB";
    public static final String TYPE_AUTP = "AUTP";
    public static final String TYPE_AUTL = "AUTL";
    public static final String TYPE_WKCP = "WKCP";
    public static final String TYPE_UMBR = "UMBR";
    public static final String TYPE_CYBR = "CYBR";
    public static final String TYPE_TERR = "TERR";

    @Id
    @Column(name = "POLICY_NUMBER", length = 12, nullable = false)
    private String policyNumber;

    @Id
    @Column(name = "SEQUENCE_NUM", nullable = false)
    private int sequenceNum;

    @Column(name = "COVERAGE_TYPE", length = 4, nullable = false)
    private String coverageType;

    @Column(name = "DESCRIPTION", length = 40)
    private String description;

    @Column(name = "COVERAGE_LIMIT", precision = 13, scale = 2)
    private BigDecimal coverageLimit = BigDecimal.ZERO;

    @Column(name = "DEDUCTIBLE", precision = 9, scale = 2)
    private BigDecimal deductible = BigDecimal.ZERO;

    @Column(name = "PREMIUM", precision = 11, scale = 2)
    private BigDecimal premium = BigDecimal.ZERO;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "STATUS", length = 2, nullable = false)
    private String status = "AC";

    @Column(name = "COINSURANCE_PCT")
    private int coinsurancePct = 100;

    @Column(name = "RATING_TERRITORY", length = 6)
    private String ratingTerritory;

    @Column(name = "CLASS_CODE", length = 5)
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

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
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
