package com.acme.insurance.pas.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class UnderwritingDecisionId implements Serializable {

    private String policyNumber;
    private LocalDate decisionDate;

    public UnderwritingDecisionId() {
    }

    public UnderwritingDecisionId(String policyNumber, LocalDate decisionDate) {
        this.policyNumber = policyNumber;
        this.decisionDate = decisionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnderwritingDecisionId that = (UnderwritingDecisionId) o;
        return Objects.equals(policyNumber, that.policyNumber)
                && Objects.equals(decisionDate, that.decisionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, decisionDate);
    }
}
