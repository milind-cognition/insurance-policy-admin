package com.acme.insurance.pas.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PremiumId implements Serializable {

    private String policyNumber;
    private int coverageSeq;
    private LocalDate termEffectiveDate;

    public PremiumId() {
    }

    public PremiumId(String policyNumber, int coverageSeq, LocalDate termEffectiveDate) {
        this.policyNumber = policyNumber;
        this.coverageSeq = coverageSeq;
        this.termEffectiveDate = termEffectiveDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PremiumId that = (PremiumId) o;
        return coverageSeq == that.coverageSeq
                && Objects.equals(policyNumber, that.policyNumber)
                && Objects.equals(termEffectiveDate, that.termEffectiveDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, coverageSeq, termEffectiveDate);
    }
}
