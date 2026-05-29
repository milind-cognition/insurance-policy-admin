package com.acme.insurance.pas.batch.config;

import com.acme.insurance.pas.batch.model.Policy;
import com.acme.insurance.pas.batch.model.PremiumRecord;
import com.acme.insurance.pas.batch.repository.PolicyRepository;
import com.acme.insurance.pas.batch.repository.PremiumRepository;
import com.acme.insurance.pas.batch.service.PremiumCalculationService;
import com.acme.insurance.pas.batch.writer.PremiumReportWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch job configuration that replaces the COBOL PREMBAT program
 * and PREMIUM-BATCH.jcl job.
 *
 * <p>Single step: read active policies -> calculate premium -> write to PREMIUMS.
 * Chunk size is configurable (default 100, vs row-by-row in COBOL).
 * Optional multi-threaded execution via TaskExecutor.
 */
@Configuration
public class BatchConfig {

    @Value("${premium.chunk-size:100}")
    private int chunkSize;

    @Value("${premium.thread-pool-size:4}")
    private int threadPoolSize;

    @Bean
    public JdbcCursorItemReader<Policy> policyReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Policy>()
                .name("policyReader")
                .dataSource(dataSource)
                .sql(PolicyRepository.SELECT_ACTIVE_POLICIES)
                .rowMapper(new PolicyRepository.PolicyRowMapper())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Policy> synchronizedPolicyReader(
            JdbcCursorItemReader<Policy> policyReader) {
        SynchronizedItemStreamReader<Policy> synchronizedReader = new SynchronizedItemStreamReader<>();
        synchronizedReader.setDelegate(policyReader);
        return synchronizedReader;
    }

    @Bean
    public ItemProcessor<Policy, PremiumRecord> premiumProcessor(
            PremiumCalculationService calculationService,
            PremiumReportWriter reportWriter) {
        return policy -> {
            reportWriter.recordRead();
            try {
                PremiumRecord record = calculationService.calculate(policy);
                return record;
            } catch (Exception ex) {
                reportWriter.recordError(policy.getPolicyNumber(), ex);
                return null; // skip this item
            }
        };
    }

    @Bean
    public ItemWriter<PremiumRecord> premiumWriter(
            PremiumRepository premiumRepository,
            PremiumReportWriter reportWriter) {
        return chunk -> {
            for (PremiumRecord record : chunk.getItems()) {
                premiumRepository.insert(record);
                reportWriter.recordWritten(record);
            }
        };
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("premium-");
        executor.setConcurrencyLimit(threadPoolSize);
        return executor;
    }

    @Bean
    public Step premiumStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            SynchronizedItemStreamReader<Policy> synchronizedPolicyReader,
                            ItemProcessor<Policy, PremiumRecord> premiumProcessor,
                            ItemWriter<PremiumRecord> premiumWriter,
                            PremiumReportWriter reportWriter,
                            TaskExecutor batchTaskExecutor) {
        return new StepBuilder("premiumStep", jobRepository)
                .<Policy, PremiumRecord>chunk(chunkSize, transactionManager)
                .reader(synchronizedPolicyReader)
                .processor(premiumProcessor)
                .writer(premiumWriter)
                .taskExecutor(batchTaskExecutor)
                .listener(reportWriter)
                .build();
    }

    @Bean
    public Job premiumBatchJob(JobRepository jobRepository, Step premiumStep) {
        return new JobBuilder("premiumBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(premiumStep)
                .build();
    }
}
