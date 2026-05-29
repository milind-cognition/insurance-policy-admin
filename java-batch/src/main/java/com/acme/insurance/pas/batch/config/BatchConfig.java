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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
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
 * Spring Batch configuration defining the premium calculation job.
 *
 * Reader  – JdbcCursorItemReader (wrapped in SynchronizedItemStreamReader for thread safety)
 *           selecting active policies (COBOL 3000-PROCESS-POLICIES).
 * Processor – PremiumCalculationService.calculate() (COBOL 3200-CALCULATE-PREMIUM).
 * Writer  – JdbcBatchItemWriter inserting into PREMIUMS (COBOL 3300-WRITE-PREMIUM-RECORD).
 *
 * Chunk size is configurable (default 100) and multi-threaded execution is enabled
 * to address the 4-hour sequential processing bottleneck of the COBOL version.
 */
@Configuration
public class BatchConfig {

    @Value("${premium.chunk-size:100}")
    private int chunkSize;

    @Value("${premium.thread-pool-size:4}")
    private int threadPoolSize;

    @Bean
    public JdbcCursorItemReader<Policy> policyCursorReader(DataSource dataSource,
                                                            PolicyRepository policyRepository) {
        return new JdbcCursorItemReaderBuilder<Policy>()
                .name("policyReader")
                .dataSource(dataSource)
                .sql(PolicyRepository.SELECT_ACTIVE_POLICIES)
                .rowMapper(policyRepository.rowMapper())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Policy> policyReader(
            JdbcCursorItemReader<Policy> policyCursorReader) {
        SynchronizedItemStreamReader<Policy> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(policyCursorReader);
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<PremiumRecord> premiumJdbcWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PremiumRecord>()
                .dataSource(dataSource)
                .sql(PremiumRepository.INSERT_PREMIUM)
                .beanMapped()
                .build();
    }

    @Bean
    public PremiumReportWriter premiumReportWriter(JdbcBatchItemWriter<PremiumRecord> premiumJdbcWriter) {
        return new PremiumReportWriter(premiumJdbcWriter);
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("premium-batch-");
        executor.setConcurrencyLimit(threadPoolSize);
        return executor;
    }

    @Bean
    public Step premiumCalculationStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       SynchronizedItemStreamReader<Policy> policyReader,
                                       PremiumCalculationService calculationService,
                                       PremiumReportWriter premiumReportWriter,
                                       TaskExecutor batchTaskExecutor) {
        return new StepBuilder("premiumCalculationStep", jobRepository)
                .<Policy, PremiumRecord>chunk(chunkSize, transactionManager)
                .reader(policyReader)
                .processor(calculationService::calculate)
                .writer(premiumReportWriter)
                .listener(premiumReportWriter)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Job premiumBatchJob(JobRepository jobRepository, Step premiumCalculationStep) {
        return new JobBuilder("premiumBatchJob", jobRepository)
                .start(premiumCalculationStep)
                .build();
    }
}
