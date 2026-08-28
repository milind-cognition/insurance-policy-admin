package com.acme.dropin.mirror;

import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.PasBackend;
import com.acme.dropin.contract.PasDate;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Policy inquiry and renewal on Java 21, Spring and an embedded database - the
 * modern implementation of what POLQRY and POLRNW do today.
 *
 * <p>It serves the contract in {@code dropin/contract}: same fields, same
 * order, same 404, same coverage ordering. Whether it produces the same
 * <em>values</em> is what {@code dropin/parity} is for.
 */
public final class MirrorPolicyAdministration implements PasBackend {

    private static final BigDecimal RATE_INCREASE = new BigDecimal("1.05");
    private static final BigDecimal RATE_INCREASE_CAP = new BigDecimal("15.00");
    private static final Set<String> RENEWABLE_STATUSES = Set.of("AC", "EX");
    private static final int MAX_COVERAGES = 20;

    private static final String POLICY_COLUMNS = """
            POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS, EFFECTIVE_DATE, EXPIRY_DATE,
            POLICYHOLDER_ID, AGENT_CODE, BRANCH_CODE, TOTAL_PREMIUM, DEDUCTIBLE,
            COVERAGE_LIMIT, INCEPTION_DATE, RENEWAL_COUNT, UW_STATUS, RISK_SCORE,
            WEB_INDICATOR, API_FLAG, LAST_UPDATED, UPDATED_BY""";

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public MirrorPolicyAdministration(MirrorDatabase database, Clock clock) {
        this.jdbc = database.jdbc();
        this.clock = clock;
    }

    @Override
    public String name() {
        return "mirror";
    }

    @Override
    public Optional<PolicyView> findPolicy(String policyNumber) {
        return jdbc.query("SELECT " + POLICY_COLUMNS + " FROM ACMEINS.POLICIES WHERE POLICY_NUMBER = ?",
                POLICY_MAPPER, policyNumber).stream().findFirst();
    }

    @Override
    public List<CoverageView> findCoverages(String policyNumber) {
        return jdbc.query("""
                SELECT POLICY_NUMBER, SEQUENCE_NUM, COVERAGE_TYPE, DESCRIPTION, COVERAGE_LIMIT,
                       DEDUCTIBLE, PREMIUM, EFFECTIVE_DATE, EXPIRY_DATE, STATUS,
                       COINSURANCE_PCT, RATING_TERRITORY, CLASS_CODE
                FROM ACMEINS.COVERAGES
                WHERE POLICY_NUMBER = ?
                ORDER BY SEQUENCE_NUM
                FETCH FIRST %d ROWS ONLY""".formatted(MAX_COVERAGES), COVERAGE_MAPPER, policyNumber);
    }

    @Override
    public RenewalResult renew(String policyNumber) {
        Optional<PolicyView> existing = findPolicy(policyNumber);
        if (existing.isEmpty()) {
            return RenewalResult.failed(RenewalResult.NOT_FOUND);
        }
        PolicyView policy = existing.get();
        if (!RENEWABLE_STATUSES.contains(policy.policyStatus())) {
            return RenewalResult.failed(RenewalResult.NOT_ELIGIBLE);
        }

        BigDecimal newPremium = policy.totalPremium().multiply(RATE_INCREASE)
                .setScale(2, RoundingMode.DOWN);
        BigDecimal increasePct = newPremium.subtract(policy.totalPremium())
                .divide(policy.totalPremium(), 4, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
        if (increasePct.compareTo(RATE_INCREASE_CAP) > 0) {
            newPremium = policy.totalPremium()
                    .multiply(BigDecimal.ONE.add(RATE_INCREASE_CAP.movePointLeft(2)))
                    .setScale(2, RoundingMode.DOWN);
        }

        PasDate newEffective = policy.expiryDate();
        PasDate newExpiry = policy.expiryDate().plusYearsByCalendar(1);
        int renewalCount = policy.renewalCount() + 1;

        jdbc.update("""
                UPDATE ACMEINS.POLICIES
                SET POLICY_STATUS = ?, EFFECTIVE_DATE = ?, EXPIRY_DATE = ?, TOTAL_PREMIUM = ?,
                    RENEWAL_COUNT = ?, UW_STATUS = ?, LAST_UPDATED = ?, UPDATED_BY = 'POLRNW'
                WHERE POLICY_NUMBER = ?""",
                "AC",
                java.sql.Date.valueOf(newEffective.toLocalDate()),
                java.sql.Date.valueOf(newExpiry.toLocalDate()),
                newPremium, renewalCount, "PN", new Timestamp(clock.millis()), policyNumber);

        jdbc.update("""
                UPDATE ACMEINS.COVERAGES
                SET EFFECTIVE_DATE = ?, EXPIRY_DATE = ?
                WHERE POLICY_NUMBER = ? AND STATUS = 'AC'""",
                java.sql.Date.valueOf(newEffective.toLocalDate()),
                java.sql.Date.valueOf(newExpiry.toLocalDate()),
                policyNumber);

        return RenewalResult.renewed(findPolicy(policyNumber).orElseThrow());
    }

    private static final RowMapper<PolicyView> POLICY_MAPPER = (ResultSet rs, int rowNum) -> new PolicyView(
            trim(rs.getString("POLICY_NUMBER")),
            trim(rs.getString("POLICY_TYPE")),
            trim(rs.getString("POLICY_STATUS")),
            date(rs, "EFFECTIVE_DATE"),
            date(rs, "EXPIRY_DATE"),
            trim(rs.getString("POLICYHOLDER_ID")),
            trim(rs.getString("AGENT_CODE")),
            trim(rs.getString("BRANCH_CODE")),
            rs.getBigDecimal("TOTAL_PREMIUM"),
            rs.getBigDecimal("DEDUCTIBLE"),
            rs.getBigDecimal("COVERAGE_LIMIT"),
            date(rs, "INCEPTION_DATE"),
            rs.getInt("RENEWAL_COUNT"),
            trim(rs.getString("UW_STATUS")),
            rs.getInt("RISK_SCORE"),
            trim(rs.getString("WEB_INDICATOR")),
            trim(rs.getString("API_FLAG")),
            rs.getTimestamp("LAST_UPDATED").getTime(),
            trim(rs.getString("UPDATED_BY")));

    private static final RowMapper<CoverageView> COVERAGE_MAPPER = (ResultSet rs, int rowNum) -> new CoverageView(
            trim(rs.getString("POLICY_NUMBER")),
            rs.getInt("SEQUENCE_NUM"),
            trim(rs.getString("COVERAGE_TYPE")),
            trim(rs.getString("DESCRIPTION")),
            rs.getBigDecimal("COVERAGE_LIMIT"),
            rs.getBigDecimal("DEDUCTIBLE"),
            rs.getBigDecimal("PREMIUM"),
            date(rs, "EFFECTIVE_DATE"),
            date(rs, "EXPIRY_DATE"),
            trim(rs.getString("STATUS")),
            rs.getInt("COINSURANCE_PCT"),
            trim(rs.getString("RATING_TERRITORY")),
            trim(rs.getString("CLASS_CODE")));

    private static PasDate date(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : PasDate.of(value.toLocalDate());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
