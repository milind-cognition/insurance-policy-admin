package com.acme.insurance.pas.dto;

import java.time.LocalDate;

public class PremiumBatchSummary {

    private int policiesRead;
    private int policiesUpdated;
    private int policiesError;
    private LocalDate runDate;

    public PremiumBatchSummary() {
    }

    public PremiumBatchSummary(int policiesRead, int policiesUpdated,
                               int policiesError, LocalDate runDate) {
        this.policiesRead = policiesRead;
        this.policiesUpdated = policiesUpdated;
        this.policiesError = policiesError;
        this.runDate = runDate;
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

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }
}
