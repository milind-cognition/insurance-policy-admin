package com.acme.insurance.pas.model;

import java.util.List;

/**
 * Response DTO for the premium batch calculation endpoint.
 * Mirrors the COBOL 4000-WRITE-SUMMARY paragraph output.
 */
public class PremiumCalcResponse {

    private int policiesRead;
    private int policiesUpdated;
    private int policiesError;
    private List<PremiumCalcResult> results;

    public PremiumCalcResponse() {
    }

    public int getPoliciesRead() {
        return policiesRead;
    }

    public void setPoliciesRead(int policiesRead) {
        this.policiesRead = policiesRead;
    }

    public int getPoliciesUpdated() {
        return policiesUpdated;
    }

    public void setPoliciesUpdated(int policiesUpdated) {
        this.policiesUpdated = policiesUpdated;
    }

    public int getPoliciesError() {
        return policiesError;
    }

    public void setPoliciesError(int policiesError) {
        this.policiesError = policiesError;
    }

    public List<PremiumCalcResult> getResults() {
        return results;
    }

    public void setResults(List<PremiumCalcResult> results) {
        this.results = results;
    }
}
