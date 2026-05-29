package com.acme.insurance.pas.model;

import java.io.Serializable;
import java.util.Objects;

public class CoverageId implements Serializable {

    private String policyNumber;
    private int sequenceNum;

    public CoverageId() {
    }

    public CoverageId(String policyNumber, int sequenceNum) {
        this.policyNumber = policyNumber;
        this.sequenceNum = sequenceNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageId that = (CoverageId) o;
        return sequenceNum == that.sequenceNum
                && Objects.equals(policyNumber, that.policyNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, sequenceNum);
    }
}
