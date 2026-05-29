package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.Policy;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides the RowMapper used by the JdbcCursorItemReader to read active
 * policies. Mirrors the SELECT in COBOL paragraph 3000-PROCESS-POLICIES
 * (PREMBAT.cbl lines 118-131).
 */
@Repository
public class PolicyRepository {

    public static final String SELECT_ACTIVE_POLICIES =
            "SELECT p.POLICY_NUMBER, p.POLICY_TYPE, p.TOTAL_PREMIUM, p.DEDUCTIBLE, "
                    + "p.COVERAGE_LIMIT, p.EFFECTIVE_DATE, p.EXPIRY_DATE, "
                    + "c.RATING_TERRITORY "
                    + "FROM POLICIES p "
                    + "LEFT JOIN COVERAGES c ON p.POLICY_NUMBER = c.POLICY_NUMBER "
                    + "AND c.SEQUENCE_NUM = 1 "
                    + "WHERE p.POLICY_STATUS = 'AC' "
                    + "ORDER BY p.POLICY_NUMBER";

    private final DataSource dataSource;

    public PolicyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static class PolicyRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyNumber(rs.getString("POLICY_NUMBER").trim());
            policy.setPolicyType(rs.getString("POLICY_TYPE").trim());
            policy.setTotalPremium(rs.getBigDecimal("TOTAL_PREMIUM"));
            policy.setDeductible(rs.getBigDecimal("DEDUCTIBLE"));
            policy.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            policy.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
            policy.setExpiryDate(rs.getDate("EXPIRY_DATE").toLocalDate());
            String territory = rs.getString("RATING_TERRITORY");
            policy.setRatingTerritory(territory != null ? territory.trim() : null);
            return policy;
        }
    }
}
