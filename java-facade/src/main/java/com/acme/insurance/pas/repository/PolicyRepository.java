package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.UnderwritingDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Policy Repository - Direct JDBC access to DB2 on z/OS.
 *
 * Uses JdbcTemplate (no JPA/Hibernate - DB2 z/OS driver has
 * compatibility issues with Hibernate dialect). Queries are
 * read-only; all writes go through CICS programs.
 *
 * NOTE: Column names match the DB2 DDL exactly. The mainframe
 * DBA team uses uppercase column names (COBOL convention).
 *
 * @author T. Nguyen (2022)
 */
@Repository
public class PolicyRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String FIND_POLICY_SQL =
            "SELECT POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS, " +
            "EFFECTIVE_DATE, EXPIRY_DATE, POLICYHOLDER_ID, " +
            "AGENT_CODE, BRANCH_CODE, TOTAL_PREMIUM, DEDUCTIBLE, " +
            "COVERAGE_LIMIT, INCEPTION_DATE, RENEWAL_COUNT, " +
            "UW_STATUS, RISK_SCORE, WEB_INDICATOR, API_FLAG, " +
            "LAST_UPDATED, UPDATED_BY " +
            "FROM ACMEINS.POLICIES " +
            "WHERE POLICY_NUMBER = ?";

    private static final String FIND_COVERAGES_SQL =
            "SELECT POLICY_NUMBER, SEQUENCE_NUM, COVERAGE_TYPE, " +
            "DESCRIPTION, COVERAGE_LIMIT, DEDUCTIBLE, PREMIUM, " +
            "EFFECTIVE_DATE, EXPIRY_DATE, STATUS, " +
            "COINSURANCE_PCT, RATING_TERRITORY, CLASS_CODE " +
            "FROM ACMEINS.COVERAGES " +
            "WHERE POLICY_NUMBER = ? " +
            "ORDER BY SEQUENCE_NUM";

    private static final String FIND_POLICY_WITH_CUSTOMER_SQL =
            "SELECT P.POLICY_NUMBER, P.POLICY_TYPE, P.TOTAL_PREMIUM, " +
            "P.COVERAGE_LIMIT, P.BRANCH_CODE, " +
            "C.CUST_TYPE, C.CREDIT_SCORE, C.RISK_TIER " +
            "FROM ACMEINS.POLICIES P " +
            "JOIN ACMEINS.POLICY_HOLDERS C ON P.POLICYHOLDER_ID = C.CUST_ID " +
            "WHERE P.POLICY_NUMBER = ?";

    private static final String COUNT_CLAIMS_SQL =
            "SELECT COUNT(*), COALESCE(SUM(INCURRED_AMOUNT), 0) " +
            "FROM ACMEINS.CLAIMS " +
            "WHERE POLICY_NUMBER = ? " +
            "AND CLAIM_DATE >= DATEADD('YEAR', -5, CURRENT_DATE)";

    private static final String SUM_BRANCH_COVERAGE_SQL =
            "SELECT COALESCE(SUM(COVERAGE_LIMIT), 0) " +
            "FROM ACMEINS.POLICIES " +
            "WHERE BRANCH_CODE = ? AND POLICY_STATUS = 'AC'";

    private static final String INSERT_UW_DECISION_SQL =
            "INSERT INTO ACMEINS.UNDERWRITING_DECISIONS " +
            "(POLICY_NUMBER, DECISION_DATE, DECISION_CODE, RISK_SCORE, " +
            "DECISION_REASON, UNDERWRITER_ID, OVERRIDE_REASON, OVERRIDE_BY) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_POLICY_UW_SQL =
            "UPDATE ACMEINS.POLICIES " +
            "SET UW_STATUS = ?, RISK_SCORE = ?, " +
            "LAST_UPDATED = CURRENT_TIMESTAMP, UPDATED_BY = ? " +
            "WHERE POLICY_NUMBER = ?";

    public Policy findByPolicyNumber(String policyNumber) {
        List<Policy> results = jdbcTemplate.query(
                FIND_POLICY_SQL,
                new Object[]{policyNumber},
                new PolicyRowMapper());
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public List<Coverage> findCoveragesByPolicyNumber(String policyNumber) {
        return jdbcTemplate.query(
                FIND_COVERAGES_SQL,
                new Object[]{policyNumber},
                new CoverageRowMapper());
    }

    public PolicyCustomerData findPolicyWithCustomer(String policyNumber) {
        List<PolicyCustomerData> results = jdbcTemplate.query(
                FIND_POLICY_WITH_CUSTOMER_SQL,
                new Object[]{policyNumber},
                new PolicyCustomerRowMapper());
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public int[] getClaimStats(String policyNumber) {
        try {
            return jdbcTemplate.queryForObject(
                    COUNT_CLAIMS_SQL,
                    new Object[]{policyNumber},
                    new RowMapper<int[]>() {
                        @Override
                        public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return new int[]{rs.getInt(1), rs.getBigDecimal(2).intValue()};
                        }
                    });
        } catch (DataAccessException e) {
            return new int[]{0, 0};
        }
    }

    public BigDecimal getBranchAccumulation(String branchCode) {
        return jdbcTemplate.queryForObject(
                SUM_BRANCH_COVERAGE_SQL,
                new Object[]{branchCode},
                BigDecimal.class);
    }

    public void insertUnderwritingDecision(UnderwritingDecision decision) {
        jdbcTemplate.update(INSERT_UW_DECISION_SQL,
                decision.getPolicyNumber(),
                decision.getDecisionDate(),
                decision.getDecisionCode(),
                decision.getRiskScore(),
                decision.getDecisionReason(),
                decision.getUnderwriterId(),
                decision.getOverrideReason(),
                decision.getOverrideBy());
    }

    public void updatePolicyUwStatus(String policyNumber, String uwStatus, int riskScore) {
        jdbcTemplate.update(UPDATE_POLICY_UW_SQL,
                uwStatus, riskScore, "UNDWRT", policyNumber);
    }

    /**
     * Holds joined policy + customer data needed for underwriting evaluation.
     */
    public static class PolicyCustomerData {
        private String policyNumber;
        private String policyType;
        private BigDecimal totalPremium;
        private BigDecimal coverageLimit;
        private String branchCode;
        private String custType;
        private int creditScore;
        private boolean creditScoreNull;
        private String riskTier;

        public String getPolicyNumber() {
            return policyNumber;
        }

        public void setPolicyNumber(String policyNumber) {
            this.policyNumber = policyNumber;
        }

        public String getPolicyType() {
            return policyType;
        }

        public void setPolicyType(String policyType) {
            this.policyType = policyType;
        }

        public BigDecimal getTotalPremium() {
            return totalPremium;
        }

        public void setTotalPremium(BigDecimal totalPremium) {
            this.totalPremium = totalPremium;
        }

        public BigDecimal getCoverageLimit() {
            return coverageLimit;
        }

        public void setCoverageLimit(BigDecimal coverageLimit) {
            this.coverageLimit = coverageLimit;
        }

        public String getBranchCode() {
            return branchCode;
        }

        public void setBranchCode(String branchCode) {
            this.branchCode = branchCode;
        }

        public String getCustType() {
            return custType;
        }

        public void setCustType(String custType) {
            this.custType = custType;
        }

        public int getCreditScore() {
            return creditScore;
        }

        public void setCreditScore(int creditScore) {
            this.creditScore = creditScore;
        }

        public boolean isCreditScoreNull() {
            return creditScoreNull;
        }

        public void setCreditScoreNull(boolean creditScoreNull) {
            this.creditScoreNull = creditScoreNull;
        }

        public String getRiskTier() {
            return riskTier;
        }

        public void setRiskTier(String riskTier) {
            this.riskTier = riskTier;
        }
    }

    private static class PolicyRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyNumber(rs.getString("POLICY_NUMBER").trim());
            policy.setPolicyType(rs.getString("POLICY_TYPE").trim());
            policy.setPolicyStatus(rs.getString("POLICY_STATUS").trim());
            policy.setEffectiveDate(rs.getDate("EFFECTIVE_DATE"));
            policy.setExpiryDate(rs.getDate("EXPIRY_DATE"));
            policy.setPolicyholderId(rs.getString("POLICYHOLDER_ID").trim());
            policy.setAgentCode(rs.getString("AGENT_CODE") != null ?
                    rs.getString("AGENT_CODE").trim() : null);
            policy.setBranchCode(rs.getString("BRANCH_CODE") != null ?
                    rs.getString("BRANCH_CODE").trim() : null);
            policy.setTotalPremium(rs.getBigDecimal("TOTAL_PREMIUM"));
            policy.setDeductible(rs.getBigDecimal("DEDUCTIBLE"));
            policy.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            policy.setInceptionDate(rs.getDate("INCEPTION_DATE"));
            policy.setRenewalCount(rs.getInt("RENEWAL_COUNT"));
            policy.setUwStatus(rs.getString("UW_STATUS").trim());
            policy.setRiskScore(rs.getInt("RISK_SCORE"));
            policy.setWebIndicator(rs.getString("WEB_INDICATOR").trim());
            policy.setApiFlag(rs.getString("API_FLAG").trim());
            policy.setLastUpdated(rs.getTimestamp("LAST_UPDATED"));
            policy.setUpdatedBy(rs.getString("UPDATED_BY").trim());
            return policy;
        }
    }

    private static class CoverageRowMapper implements RowMapper<Coverage> {
        @Override
        public Coverage mapRow(ResultSet rs, int rowNum) throws SQLException {
            Coverage coverage = new Coverage();
            coverage.setPolicyNumber(rs.getString("POLICY_NUMBER").trim());
            coverage.setSequenceNum(rs.getInt("SEQUENCE_NUM"));
            coverage.setCoverageType(rs.getString("COVERAGE_TYPE").trim());
            coverage.setDescription(rs.getString("DESCRIPTION") != null ?
                    rs.getString("DESCRIPTION").trim() : null);
            coverage.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            coverage.setDeductible(rs.getBigDecimal("DEDUCTIBLE"));
            coverage.setPremium(rs.getBigDecimal("PREMIUM"));
            coverage.setEffectiveDate(rs.getDate("EFFECTIVE_DATE"));
            coverage.setExpiryDate(rs.getDate("EXPIRY_DATE"));
            coverage.setStatus(rs.getString("STATUS").trim());
            coverage.setCoinsurancePct(rs.getInt("COINSURANCE_PCT"));
            coverage.setRatingTerritory(rs.getString("RATING_TERRITORY") != null ?
                    rs.getString("RATING_TERRITORY").trim() : null);
            coverage.setClassCode(rs.getString("CLASS_CODE") != null ?
                    rs.getString("CLASS_CODE").trim() : null);
            return coverage;
        }
    }

    private static class PolicyCustomerRowMapper implements RowMapper<PolicyCustomerData> {
        @Override
        public PolicyCustomerData mapRow(ResultSet rs, int rowNum) throws SQLException {
            PolicyCustomerData data = new PolicyCustomerData();
            data.setPolicyNumber(rs.getString("POLICY_NUMBER").trim());
            data.setPolicyType(rs.getString("POLICY_TYPE").trim());
            data.setTotalPremium(rs.getBigDecimal("TOTAL_PREMIUM"));
            data.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            String branchCode = rs.getString("BRANCH_CODE");
            data.setBranchCode(branchCode != null ? branchCode.trim() : null);
            String custType = rs.getString("CUST_TYPE");
            data.setCustType(custType != null ? custType.trim() : null);
            int creditScore = rs.getInt("CREDIT_SCORE");
            data.setCreditScoreNull(rs.wasNull());
            data.setCreditScore(creditScore);
            String riskTier = rs.getString("RISK_TIER");
            data.setRiskTier(riskTier != null ? riskTier.trim() : null);
            return data;
        }
    }
}
