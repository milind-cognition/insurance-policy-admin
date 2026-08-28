package com.acme.dropin.contract;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/** The coverage resource exactly as the incumbent facade serializes it. */
@JsonPropertyOrder({
        "policyNumber", "sequenceNum", "coverageType", "description", "coverageLimit",
        "deductible", "premium", "effectiveDate", "expiryDate", "status",
        "coinsurancePct", "ratingTerritory", "classCode"})
public record CoverageView(
        String policyNumber,
        int sequenceNum,
        String coverageType,
        String description,
        BigDecimal coverageLimit,
        BigDecimal deductible,
        BigDecimal premium,
        PasDate effectiveDate,
        PasDate expiryDate,
        String status,
        int coinsurancePct,
        String ratingTerritory,
        String classCode) {

    public CoverageView {
        coverageLimit = Money.pin(coverageLimit);
        deductible = Money.pin(deductible);
        premium = Money.pin(premium);
    }

    public CoverageView withTerm(PasDate newEffective, PasDate newExpiry) {
        return new CoverageView(policyNumber, sequenceNum, coverageType, description,
                coverageLimit, deductible, premium, newEffective, newExpiry, status,
                coinsurancePct, ratingTerritory, classCode);
    }
}
