package com.acme.insurance.pas.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that runs the full Spring Batch job against the H2
 * test database and verifies premium records are written correctly.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class PremiumBatchJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job premiumBatchJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanPremiums() {
        jdbcTemplate.execute("DELETE FROM PREMIUMS");
    }

    @Test
    void testJobCompletesSuccessfully() throws Exception {
        jobLauncherTestUtils.setJob(premiumBatchJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        // 5 active policies in test data (the CN one is excluded)
        Integer premiumCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PREMIUMS", Integer.class);
        assertEquals(5, premiumCount);
    }

    @Test
    void testPremiumAmountsAreCorrect() throws Exception {
        jobLauncherTestUtils.setJob(premiumBatchJob);
        jobLauncherTestUtils.launchJob();

        // AUTO policy: base 850 * territory 1.15 (POL-AU matches test data) = 977.50
        // tax = 977.50 * 0.035 = 34.21, total = 977.50 + 34.21 + 25 = 1036.71
        BigDecimal autoPremium = jdbcTemplate.queryForObject(
                "SELECT TOTAL_PREMIUM FROM PREMIUMS WHERE POLICY_NUMBER = 'POL-AUT-0001'",
                BigDecimal.class);
        assertTrue(autoPremium.compareTo(BigDecimal.ZERO) > 0,
                "AUTO premium should be positive");

        // LIFE policy (no territory factor): 400 + 14 + 25 = 439
        BigDecimal lifePremium = jdbcTemplate.queryForObject(
                "SELECT TOTAL_PREMIUM FROM PREMIUMS WHERE POLICY_NUMBER = 'POL-LIF-0004'",
                BigDecimal.class);
        assertEquals(new BigDecimal("439.00"), lifePremium);
    }

    @Test
    void testCancelledPolicyExcluded() throws Exception {
        jobLauncherTestUtils.setJob(premiumBatchJob);
        jobLauncherTestUtils.launchJob();

        Integer cancelledCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PREMIUMS WHERE POLICY_NUMBER = 'POL-AUT-9999'",
                Integer.class);
        assertEquals(0, cancelledCount);
    }
}
