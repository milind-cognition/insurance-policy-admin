package com.acme.insurance.pas.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyNumber) {
        super("Policy not found: " + policyNumber);
    }
}
