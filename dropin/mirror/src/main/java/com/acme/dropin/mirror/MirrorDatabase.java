package com.acme.dropin.mirror;

import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PolicyView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * The mirror's embedded database: H2 in DB2 compatibility mode, with the schema
 * translated from the production DDL by {@link Db2Ddl}.
 *
 * <p>A file-backed URL is available on purpose. It is what lets the
 * <em>unmodified</em> Spring Boot 1.5 facade be pointed at the mirror's data
 * with nothing but a datasource property, which is the drop-in claim in its
 * least deniable form.
 */
public final class MirrorDatabase {

    public static final String IN_MEMORY_URL = "jdbc:h2:mem:pasmirror;DB_CLOSE_DELAY=-1;MODE=DB2";

    private final JdbcTemplate jdbc;

    public MirrorDatabase(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public static MirrorDatabase inMemory() {
        return of(IN_MEMORY_URL);
    }

    public static MirrorDatabase of(String jdbcUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        MirrorDatabase database = new MirrorDatabase(dataSource);
        // The mirror owns this database outright; starting it always starts from
        // the DDL, so a demo can be re-run without leftovers deciding the answer.
        database.jdbc.execute("DROP ALL OBJECTS");
        database.createSchema();
        return database;
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public void createSchema() {
        for (String statement : Db2Ddl.fromRepository().statements()) {
            jdbc.execute(statement);
        }
    }

    /** Loads the demo population, and only that - no other rows exist. */
    public MirrorDatabase loadDemoData() {
        List<PolicyView> policies = DemoDataset.policies();
        Map<String, List<CoverageView>> coverages = DemoDataset.coverages(policies);
        jdbc.update("DELETE FROM ACMEINS.COVERAGES");
        jdbc.update("DELETE FROM ACMEINS.POLICIES");
        for (PolicyView policy : policies) {
            insert(policy);
            for (CoverageView coverage : coverages.get(policy.policyNumber())) {
                insert(coverage);
            }
        }
        return this;
    }

    public void insert(PolicyView policy) {
        jdbc.update("""
                INSERT INTO ACMEINS.POLICIES (
                    POLICY_NUMBER, POLICY_TYPE, POLICY_STATUS, EFFECTIVE_DATE, EXPIRY_DATE,
                    POLICYHOLDER_ID, AGENT_CODE, BRANCH_CODE, TOTAL_PREMIUM, DEDUCTIBLE,
                    COVERAGE_LIMIT, INCEPTION_DATE, RENEWAL_COUNT, UW_STATUS, RISK_SCORE,
                    WEB_INDICATOR, API_FLAG, LAST_UPDATED, UPDATED_BY)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                policy.policyNumber(), policy.policyType(), policy.policyStatus(),
                java.sql.Date.valueOf(policy.effectiveDate().toLocalDate()),
                java.sql.Date.valueOf(policy.expiryDate().toLocalDate()),
                policy.policyholderId(), policy.agentCode(), policy.branchCode(),
                policy.totalPremium(), policy.deductible(), policy.coverageLimit(),
                java.sql.Date.valueOf(policy.inceptionDate().toLocalDate()),
                policy.renewalCount(), policy.uwStatus(), policy.riskScore(),
                policy.webIndicator(), policy.apiFlag(),
                new Timestamp(policy.lastUpdated()), policy.updatedBy());
    }

    public void insert(CoverageView coverage) {
        jdbc.update("""
                INSERT INTO ACMEINS.COVERAGES (
                    POLICY_NUMBER, SEQUENCE_NUM, COVERAGE_TYPE, DESCRIPTION, COVERAGE_LIMIT,
                    DEDUCTIBLE, PREMIUM, EFFECTIVE_DATE, EXPIRY_DATE, STATUS,
                    COINSURANCE_PCT, RATING_TERRITORY, CLASS_CODE)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                coverage.policyNumber(), coverage.sequenceNum(), coverage.coverageType(),
                coverage.description(), coverage.coverageLimit(), coverage.deductible(),
                coverage.premium(),
                java.sql.Date.valueOf(coverage.effectiveDate().toLocalDate()),
                java.sql.Date.valueOf(coverage.expiryDate().toLocalDate()),
                coverage.status(), coverage.coinsurancePct(), coverage.ratingTerritory(),
                coverage.classCode());
    }
}
