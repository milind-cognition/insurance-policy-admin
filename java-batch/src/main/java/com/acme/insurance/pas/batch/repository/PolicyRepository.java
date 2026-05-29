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
            "SELECT P.POLICY_NUMBER, P.POLICY_TYPE, P.POLICY_STATUS, "
                    + "P.EFFECTIVE_DATE, P.EXPIRY_DATE, "
                    + "P.TOTAL_PREMIUM, P.DEDUCTIBLE, P.COVERAGE_LIMIT, "
                    + "C.RATING_TERRITORY "
                    + "FROM POLICIES P "
                    + "LEFT JOIN COVERAGES C "
                    + "ON P.POLICY_NUMBER = C.POLICY_NUMBER AND C.SEQUENCE_NUM = 1 "
                    + "WHERE P.POLICY_STATUS = 'AC' "
                    + "ORDER BY P.POLICY_NUMBER";

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
            String territory = rs.getString("RATING_TERRITORY");
            if (territory != null) {
                policy.setTerritoryCode(territory.trim());
            }
            return policy;
        }
    }
}
