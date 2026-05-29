package com.acme.insurance.pas.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for the PREMIUMS table
 * (POLICY_NUMBER, COVERAGE_SEQ, TERM_EFFECTIVE_DATE).
 */
public class PremiumId implements Serializable {

    private String policyNumber;
    private Integer coverageSeq;
    private LocalDate termEffectiveDate;

    public PremiumId() {
    }

    public PremiumId(String policyNumber, Integer coverageSeq,
                     LocalDate termEffectiveDate) {
        this.policyNumber = policyNumber;
        this.coverageSeq = coverageSeq;
        this.termEffectiveDate = termEffectiveDate;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Integer getCoverageSeq() {
        return coverageSeq;
    }

    public LocalDate getTermEffectiveDate() {
        return termEffectiveDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PremiumId that = (PremiumId) o;
        return Objects.equals(policyNumber, that.policyNumber)
                && Objects.equals(coverageSeq, that.coverageSeq)
                && Objects.equals(termEffectiveDate, that.termEffectiveDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, coverageSeq, termEffectiveDate);
    }
}
