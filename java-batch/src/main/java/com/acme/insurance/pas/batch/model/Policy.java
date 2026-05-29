package com.acme.insurance.pas.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps the fields from cobol/copybooks/POLICY-RECORD.cpy and the
 * ACMEINS.POLICIES DB2 table that are relevant to premium calculation.
 */
public class Policy {

    private String policyNumber;       // CHAR(12)
    private String policyType;         // CHAR(3): AUT, HOM, COM, LIF, HLT
    private String policyStatus;       // CHAR(2): AC, PN, CN, EX, LP
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private BigDecimal totalPremium;
    private BigDecimal deductible;
    private BigDecimal coverageLimit;
    private String ratingTerritory;    // from COVERAGES.RATING_TERRITORY

    public Policy() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
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

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public String getRatingTerritory() {
        return ratingTerritory;
    }

    public void setRatingTerritory(String ratingTerritory) {
        this.ratingTerritory = ratingTerritory;
    }
}
