package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.Policy;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class PolicyRepository {

    private final DataSource dataSource;

    public PolicyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static final String ACTIVE_POLICIES_SQL =
            "SELECT POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS, "
                    + "EFFECTIVE_DATE, EXPIRY_DATE, "
                    + "TOTAL_PREMIUM, DEDUCTIBLE, COVERAGE_LIMIT "
                    + "FROM POLICIES "
                    + "WHERE POLICY_STATUS = 'AC' "
                    + "ORDER BY POLICY_NUMBER";

    public static class PolicyRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyNumber(rs.getString("POLICY_NUMBER").trim());
            policy.setPolicyType(rs.getString("POLICY_TYPE").trim());
            policy.setPolicyStatus(rs.getString("POLICY_STATUS").trim());
            policy.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
            policy.setExpiryDate(rs.getDate("EXPIRY_DATE").toLocalDate());
            policy.setTotalPremium(rs.getBigDecimal("TOTAL_PREMIUM"));
            policy.setDeductible(rs.getBigDecimal("DEDUCTIBLE"));
            policy.setCoverageLimit(rs.getBigDecimal("COVERAGE_LIMIT"));
            return policy;
        }
    }
}
