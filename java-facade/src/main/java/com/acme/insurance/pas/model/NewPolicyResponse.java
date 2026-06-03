package com.acme.insurance.pas.model;

/**
 * Response DTO for new policy creation.
 * Maps to the confirmation screen output from COBOL program POLNEW (paragraph 8000-SEND-CONFIRMATION).
 */
public class NewPolicyResponse {

    private String policyNumber;
    private String policyStatus;
    private String message;
    private int coveragesCreated;

    public NewPolicyResponse() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCoveragesCreated() {
        return coveragesCreated;
    }

    public void setCoveragesCreated(int coveragesCreated) {
        this.coveragesCreated = coveragesCreated;
    }
}
