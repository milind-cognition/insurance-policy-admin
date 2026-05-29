package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.Policy;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for reading active policies from the POLICIES table,
 * joined with COVERAGES to retrieve the territory code.
 * Corresponds to COBOL paragraph 3000-PROCESS-POLICIES cursor query.
 */
@Repository
public class PolicyRepository {

    public static final String SELECT_ACTIVE_POLICIES =
            "SELECT P.POLICY_NUMBER, P.POLICY_TYPE, P.POLICY_STATUS, " +
            "P.TOTAL_PREMIUM, P.DEDUCTIBLE, P.COVERAGE_LIMIT, " +
            "P.EFFECTIVE_DATE, P.EXPIRY_DATE, " +
            "C.RATING_TERRITORY " +
            "FROM POLICIES P " +
            "LEFT JOIN COVERAGES C " +
            "ON P.POLICY_NUMBER = C.POLICY_NUMBER AND C.SEQUENCE_NUM = 1 " +
            "WHERE P.POLICY_STATUS = 'AC' " +
            "ORDER BY P.POLICY_NUMBER";

    public RowMapper<Policy> rowMapper() {
        return new PolicyRowMapper();
    }

    private static class PolicyRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyNumber(rs.getString("POLICY_NUMBER"));
            policy.setPolicyType(rs.getString("POLICY_TYPE"));
            policy.setPolicyStatus(rs.getString("POLICY_STATUS"));
            policy.setTotalPremium(rs.getBigDecimal("TOTAL_PREMIUM"));
            policy.setDeductible(rs.getBigDecimal("DEDUCTIBLE"));
            policy.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            policy.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
            policy.setExpiryDate(rs.getDate("EXPIRY_DATE").toLocalDate());
            String territory = rs.getString("RATING_TERRITORY");
            if (territory != null) {
                policy.setTerritoryCode(territory.trim());
            }
            return policy;
        }
    }
}
