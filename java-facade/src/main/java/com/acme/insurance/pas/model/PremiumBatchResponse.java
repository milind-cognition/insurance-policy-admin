package com.acme.insurance.pas.model;

/**
 * Response DTO for the premium batch calculation endpoint.
 * Mirrors the summary counters from COBOL program PREMBAT paragraph 4000-WRITE-SUMMARY.
 */
public class PremiumBatchResponse {

    private int policiesRead;
    private int policiesUpdated;
    private int policiesError;
    private String message;

    public PremiumBatchResponse() {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
