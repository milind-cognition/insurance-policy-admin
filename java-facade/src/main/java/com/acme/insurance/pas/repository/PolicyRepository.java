package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.model.Premium;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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

    private static final String FIND_ACTIVE_POLICIES_SQL =
            "SELECT POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS, " +
            "EFFECTIVE_DATE, EXPIRY_DATE, POLICYHOLDER_ID, " +
            "AGENT_CODE, BRANCH_CODE, TOTAL_PREMIUM, DEDUCTIBLE, " +
            "COVERAGE_LIMIT, INCEPTION_DATE, RENEWAL_COUNT, " +
            "UW_STATUS, RISK_SCORE, WEB_INDICATOR, API_FLAG, " +
            "LAST_UPDATED, UPDATED_BY " +
            "FROM ACMEINS.POLICIES " +
            "WHERE POLICY_STATUS = 'AC' " +
            "ORDER BY POLICY_NUMBER";

    private static final String INSERT_PREMIUM_SQL =
            "INSERT INTO ACMEINS.PREMIUMS " +
            "(POLICY_NUMBER, COVERAGE_SEQ, TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE, " +
            "BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR, EXPERIENCE_MOD, " +
            "SCHEDULE_MOD, DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT, " +
            "TOTAL_PREMIUM, INSTALLMENT_CODE, CALC_DATE, CALC_BY) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_COVERAGES_SQL =
            "SELECT POLICY_NUMBER, SEQUENCE_NUM, COVERAGE_TYPE, " +
            "DESCRIPTION, COVERAGE_LIMIT, DEDUCTIBLE, PREMIUM, " +
            "EFFECTIVE_DATE, EXPIRY_DATE, STATUS, " +
            "COINSURANCE_PCT, RATING_TERRITORY, CLASS_CODE " +
            "FROM ACMEINS.COVERAGES " +
            "WHERE POLICY_NUMBER = ? " +
            "ORDER BY SEQUENCE_NUM";

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

    public List<Policy> findActivePolicies() {
        return jdbcTemplate.query(
                FIND_ACTIVE_POLICIES_SQL,
                new PolicyRowMapper());
    }

    public void insertPremium(Premium premium) {
        jdbcTemplate.update(INSERT_PREMIUM_SQL,
                premium.getPolicyNumber(),
                premium.getCoverageSeq(),
                premium.getTermEffectiveDate(),
                premium.getTermExpiryDate(),
                premium.getBaseRate(),
                premium.getTerritoryFactor(),
                premium.getClassFactor(),
                premium.getExperienceMod(),
                premium.getScheduleMod(),
                premium.getDiscountPct(),
                premium.getSurchargeAmt(),
                premium.getTaxAmt(),
                premium.getTotalPremium(),
                premium.getInstallmentCode(),
                premium.getCalcDate(),
                premium.getCalcBy());
    }

    public List<Coverage> findCoveragesByPolicyNumber(String policyNumber) {
        return jdbcTemplate.query(
                FIND_COVERAGES_SQL,
                new Object[]{policyNumber},
                new CoverageRowMapper());
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
}
