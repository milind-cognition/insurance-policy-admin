package com.acme.insurance.pas.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the ENDORSEMENTS table
 * (POLICY_NUMBER, ENDORSEMENT_SEQ).
 */
public class EndorsementId implements Serializable {

    private String policyNumber;
    private Integer endorsementSeq;

    public EndorsementId() {
    }

    public EndorsementId(String policyNumber, Integer endorsementSeq) {
        this.policyNumber = policyNumber;
        this.endorsementSeq = endorsementSeq;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Integer getEndorsementSeq() {
        return endorsementSeq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndorsementId that = (EndorsementId) o;
        return Objects.equals(policyNumber, that.policyNumber)
                && Objects.equals(endorsementSeq, that.endorsementSeq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyNumber, endorsementSeq);
    }
}
