package com.acme.insurance.pas.batch.writer;

import com.acme.insurance.pas.batch.model.PremiumRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a summary report equivalent to COBOL paragraph 4000-WRITE-SUMMARY
 * (PREMBAT.cbl lines 234-250).
 *
 * Tracks policies processed, updated, and errors; logs a summary at step completion.
 */
@Component
public class PremiumReportWriter implements ItemWriter<PremiumRecord>, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(PremiumReportWriter.class);

    private final AtomicInteger policiesRead = new AtomicInteger(0);
    private final AtomicInteger policiesUpdated = new AtomicInteger(0);
    private final AtomicInteger policiesError = new AtomicInteger(0);

    private final ItemWriter<PremiumRecord> delegate;

    public PremiumReportWriter(ItemWriter<PremiumRecord> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        policiesRead.set(0);
        policiesUpdated.set(0);
        policiesError.set(0);
        log.info("========================================================");
        log.info("ACME INSURANCE - PREMIUM BATCH CALCULATION");
        log.info("========================================================");
    }

    @Override
    public void write(Chunk<? extends PremiumRecord> items) throws Exception {
        policiesRead.addAndGet(items.size());
        try {
            delegate.write(items);
            policiesUpdated.addAndGet(items.size());
            for (PremiumRecord record : items) {
                log.info("Policy {} premium: {}", record.getPolicyNumber(), record.getTotalPremium());
            }
        } catch (Exception e) {
            policiesError.addAndGet(items.size());
            throw e;
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("========================================================");
        log.info("TOTAL POLICIES READ:    {}", policiesRead.get());
        log.info("TOTAL POLICIES UPDATED: {}", policiesUpdated.get());
        log.info("TOTAL ERRORS:           {}", policiesError.get());
        log.info("========================================================");

        stepExecution.getExecutionContext().putInt("policiesRead", policiesRead.get());
        stepExecution.getExecutionContext().putInt("policiesUpdated", policiesUpdated.get());
        stepExecution.getExecutionContext().putInt("policiesError", policiesError.get());

        if (policiesError.get() > 0) {
            return new ExitStatus("COMPLETED_WITH_ERRORS");
        }
        return ExitStatus.COMPLETED;
    }

    public int getPoliciesRead() {
        return policiesRead.get();
    }

    public int getPoliciesUpdated() {
        return policiesUpdated.get();
    }

    public int getPoliciesError() {
        return policiesError.get();
    }
}
