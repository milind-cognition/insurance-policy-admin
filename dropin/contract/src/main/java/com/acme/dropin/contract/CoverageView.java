package com.acme.dropin.contract;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String status,
        int coinsurancePct,
        String ratingTerritory,
        String classCode) {

    public CoverageView {
        coverageLimit = Money.pin(coverageLimit);
        deductible = Money.pin(deductible);
        premium = Money.pin(premium);
    }

    public CoverageView withTerm(LocalDate newEffective, LocalDate newExpiry) {
        return new CoverageView(policyNumber, sequenceNum, coverageType, description,
                coverageLimit, deductible, premium, newEffective, newExpiry, status,
                coinsurancePct, ratingTerritory, classCode);
    }
}
