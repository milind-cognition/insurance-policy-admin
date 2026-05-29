package com.acme.insurance.pas.batch.writer;

import com.acme.insurance.pas.batch.model.PremiumRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates the summary report that COBOL paragraph 4000-WRITE-SUMMARY
 * (PREMBAT.cbl lines 234-250) wrote to the PREMRPT DD.
 * Output goes to the application log instead of a flat file.
 */
@Component
public class PremiumReportWriter {

    private static final Logger log = LoggerFactory.getLogger(PremiumReportWriter.class);

    private final AtomicLong policiesRead = new AtomicLong();
    private final AtomicLong policiesUpdated = new AtomicLong();
    private final AtomicLong policiesError = new AtomicLong();

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        policiesRead.set(0);
        policiesUpdated.set(0);
        policiesError.set(0);
        log.info("========================================================");
        log.info("ACME INSURANCE - PREMIUM BATCH CALCULATION");
        log.info("========================================================");
    }

    public void recordRead() {
        policiesRead.incrementAndGet();
    }

    public void recordWritten(PremiumRecord record) {
        policiesUpdated.incrementAndGet();
        log.debug("{} PREMIUM: {}", record.getPolicyNumber(), record.getTotalPremium());
    }

    public void recordError(String policyNumber, Exception ex) {
        policiesError.incrementAndGet();
        log.error("Error processing policy {}: {}", policyNumber, ex.getMessage());
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        log.info("========================================================");
        log.info("TOTAL POLICIES READ:    {}", policiesRead.get());
        log.info("TOTAL POLICIES UPDATED: {}", policiesUpdated.get());
        log.info("TOTAL ERRORS:           {}", policiesError.get());
        log.info("========================================================");
    }

    public long getPoliciesRead() {
        return policiesRead.get();
    }

    public long getPoliciesUpdated() {
        return policiesUpdated.get();
    }

    public long getPoliciesError() {
        return policiesError.get();
    }
}
