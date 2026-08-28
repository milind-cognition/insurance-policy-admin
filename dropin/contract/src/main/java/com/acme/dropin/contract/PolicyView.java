package com.acme.dropin.contract;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The policy resource exactly as the incumbent facade serializes it.
 *
 * <p>Component order is the JSON property order; the money fields are pinned to
 * scale 2 because DB2 {@code DECIMAL(n,2)} and COBOL {@code PIC S9(n)V99 COMP-3}
 * both carry two decimal places and JSON renders {@code 1250.00} differently
 * from {@code 1250.0}. A caller comparing response bodies would see that.
 */
@JsonPropertyOrder({
        "policyNumber", "policyType", "policyStatus", "effectiveDate", "expiryDate",
        "policyholderId", "agentCode", "branchCode", "totalPremium", "deductible",
        "coverageLimit", "inceptionDate", "renewalCount", "uwStatus", "riskScore",
        "webIndicator", "apiFlag", "lastUpdated", "updatedBy"})
public record PolicyView(
        String policyNumber,
        String policyType,
        String policyStatus,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String policyholderId,
        String agentCode,
        String branchCode,
        BigDecimal totalPremium,
        BigDecimal deductible,
        BigDecimal coverageLimit,
        LocalDate inceptionDate,
        int renewalCount,
        String uwStatus,
        int riskScore,
        String webIndicator,
        String apiFlag,
        long lastUpdated,
        String updatedBy) {

    public PolicyView {
        totalPremium = Money.pin(totalPremium);
        deductible = Money.pin(deductible);
        coverageLimit = Money.pin(coverageLimit);
    }

    public PolicyView withPremium(BigDecimal premium) {
        return new PolicyView(policyNumber, policyType, policyStatus, effectiveDate, expiryDate,
                policyholderId, agentCode, branchCode, premium, deductible, coverageLimit,
                inceptionDate, renewalCount, uwStatus, riskScore, webIndicator, apiFlag,
                lastUpdated, updatedBy);
    }

    public PolicyView renewedTo(LocalDate newEffective, LocalDate newExpiry, BigDecimal premium,
                                int renewals, String status, String underwritingStatus) {
        return new PolicyView(policyNumber, policyType, status, newEffective, newExpiry,
                policyholderId, agentCode, branchCode, premium, deductible, coverageLimit,
                inceptionDate, renewals, underwritingStatus, riskScore, webIndicator, apiFlag,
                lastUpdated, updatedBy);
    }
}
