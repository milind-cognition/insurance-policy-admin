package com.acme.dropin.mirror;

import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PasDate;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mirror's own tests: it reads and renews policies and serves the contract.
 *
 * <p>These pass. Whether the mirror agrees with the incumbent is a different
 * question, asked by {@code dropin/parity} - which is the point the demo is
 * making about unit tests.
 */
class MirrorPolicyAdministrationTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.ofEpochMilli(DemoDataset.FIXED_LAST_UPDATED), ZoneOffset.UTC);

    private static MirrorDatabase database;
    private MirrorPolicyAdministration mirror;

    @BeforeAll
    static void createDatabase() {
        database = MirrorDatabase.of("jdbc:h2:mem:mirrortest;DB_CLOSE_DELAY=-1;MODE=DB2");
    }

    @BeforeEach
    void loadData() {
        database.loadDemoData();
        mirror = new MirrorPolicyAdministration(database, FIXED);
    }

    @Test
    void schemaComesFromTheProductionDdl() {
        Integer columns = database.jdbc().queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'ACMEINS' AND TABLE_NAME = 'POLICIES'""", Integer.class);
        assertEquals(19, columns, "POLICIES has 19 columns in sql/ddl/create-tables.sql");
    }

    @Test
    void inquiryServesTheContract() {
        PolicyView policy = mirror.findPolicy("PAS-00000001").orElseThrow();
        assertEquals("PAS-00000001", policy.policyNumber());
        assertEquals(new BigDecimal("501.37"), policy.totalPremium());
        assertEquals(DemoDataset.FIXED_LAST_UPDATED, policy.lastUpdated());
        assertTrue(mirror.findPolicy("PAS-99999999").isEmpty());
    }

    @Test
    void coveragesAreOrderedBySequenceNumber() {
        List<CoverageView> coverages = mirror.findCoverages("PAS-00000001");
        for (int i = 0; i < coverages.size(); i++) {
            assertEquals(i + 1, coverages.get(i).sequenceNum());
        }
    }

    @Test
    void renewalMovesTheTermAndRaisesThePremium() {
        PolicyView renewed = mirror.renew("PAS-00000002").policy();
        assertEquals("AC", renewed.policyStatus());
        assertEquals("PN", renewed.uwStatus());
        assertEquals("POLRNW", renewed.updatedBy());
        assertEquals(PasDate.of(20270301), renewed.expiryDate());
        // 502.74 * 1.05 = 527.877
        assertEquals(new BigDecimal("527.87"), renewed.totalPremium());
    }

    @Test
    void anUnknownPolicyCannotBeRenewed() {
        assertEquals(RenewalResult.NOT_FOUND, mirror.renew("PAS-99999999").errorMessage());
    }
}
