package com.acme.dropin.contract;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The join between the two projections of the same contract: the JSON/XML
 * field a caller sees on the service boundary, and the COMMAREA field a CICS
 * program sees underneath it.
 *
 * <p>Names differ in places ({@code coverageLimit} is {@code POLICY-LIMIT},
 * {@code webIndicator} is {@code POLICY-WEB-IND}), which is exactly why the
 * mapping is code with a test rather than prose in a wiki.
 */
public final class CommareaMapping {

    public static final Map<String, String> POLICY = policyMapping();
    public static final Map<String, String> COVERAGE = coverageMapping();

    private CommareaMapping() {
    }

    private static Map<String, String> policyMapping() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("policyNumber", "POLICY-NUMBER");
        m.put("policyType", "POLICY-TYPE");
        m.put("policyStatus", "POLICY-STATUS");
        m.put("effectiveDate", "POLICY-EFFECTIVE-DATE");
        m.put("expiryDate", "POLICY-EXPIRY-DATE");
        m.put("policyholderId", "POLICY-HOLDER-ID");
        m.put("agentCode", "POLICY-AGENT-CODE");
        m.put("branchCode", "POLICY-BRANCH-CODE");
        m.put("totalPremium", "POLICY-TOTAL-PREMIUM");
        m.put("deductible", "POLICY-DEDUCTIBLE");
        m.put("coverageLimit", "POLICY-LIMIT");
        m.put("inceptionDate", "POLICY-INCEPTION-DATE");
        m.put("renewalCount", "POLICY-RENEWAL-COUNT");
        m.put("uwStatus", "POLICY-UW-STATUS");
        m.put("riskScore", "POLICY-RISK-SCORE");
        m.put("webIndicator", "POLICY-WEB-IND");
        m.put("apiFlag", "POLICY-API-FLAG");
        m.put("lastUpdated", "POLICY-LAST-UPDATED");
        m.put("updatedBy", "POLICY-UPDATED-BY");
        return Map.copyOf(m);
    }

    private static Map<String, String> coverageMapping() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("policyNumber", "COV-POLICY-NUMBER");
        m.put("sequenceNum", "COV-SEQUENCE-NUM");
        m.put("coverageType", "COV-TYPE-CODE");
        m.put("description", "COV-DESCRIPTION");
        m.put("coverageLimit", "COV-LIMIT");
        m.put("deductible", "COV-DEDUCTIBLE");
        m.put("premium", "COV-PREMIUM");
        m.put("effectiveDate", "COV-EFFECTIVE-DATE");
        m.put("expiryDate", "COV-EXPIRY-DATE");
        m.put("status", "COV-STATUS");
        m.put("coinsurancePct", "COV-COINSURANCE-PCT");
        m.put("ratingTerritory", "COV-RATING-TERRITORY");
        m.put("classCode", "COV-CLASS-CODE");
        return Map.copyOf(m);
    }

    public static Map<String, String> forSchema(String schemaName) {
        return switch (schemaName) {
            case "Policy" -> POLICY;
            case "Coverage" -> COVERAGE;
            default -> throw new IllegalArgumentException("Unknown schema " + schemaName);
        };
    }

    static CopybookField copybookFieldFor(String schemaName, String jsonField, Copybook copybook) {
        String cobolName = forSchema(schemaName).get(jsonField);
        return cobolName == null ? null : copybook.field(cobolName);
    }
}
