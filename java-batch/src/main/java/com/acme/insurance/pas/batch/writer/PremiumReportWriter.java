package com.acme.insurance.pas.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class PremiumReportWriter {

    private static final Logger log = LoggerFactory.getLogger(PremiumReportWriter.class);

    public void writeSummary(StepExecution stepExecution) {
        long read = stepExecution.getReadCount();
        long written = stepExecution.getWriteCount();
        long errors = stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount();

        log.info("========================================================");
        log.info("ACME INSURANCE - PREMIUM BATCH CALCULATION SUMMARY");
        log.info("========================================================");
        log.info("TOTAL POLICIES READ:    {}", read);
        log.info("TOTAL POLICIES UPDATED: {}", written);
        log.info("TOTAL ERRORS:           {}", errors);
        log.info("========================================================");
    }
}
