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
            "SELECT POLICY_NUMBER, POLICY_TYPE, TOTAL_PREMIUM, DEDUCTIBLE, "
                    + "COVERAGE_LIMIT, EFFECTIVE_DATE, EXPIRY_DATE "
                    + "FROM POLICIES "
                    + "WHERE POLICY_STATUS = 'AC' "
                    + "ORDER BY POLICY_NUMBER";

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
            return policy;
        }
    }
}
