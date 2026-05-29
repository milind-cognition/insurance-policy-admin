package com.acme.insurance.pas.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the COVERAGES table (POLICY_NUMBER, SEQUENCE_NUM).
 */
public class CoverageId implements Serializable {

    private String policyNumber;
    private Integer sequenceNum;

    public CoverageId() {
    }

    public CoverageId(String policyNumber, Integer sequenceNum) {
        this.policyNumber = policyNumber;
        this.sequenceNum = sequenceNum;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Integer getSequenceNum() {
        return sequenceNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageId that = (CoverageId) o;
        return Objects.equals(policyNumber, that.policyNumber)
                && Objects.equals(sequenceNum, that.sequenceNum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, sequenceNum);
    }
}
