package com.acme.insurance.pas.batch.config;

import com.acme.insurance.pas.batch.model.Policy;
import com.acme.insurance.pas.batch.model.PremiumRecord;
import com.acme.insurance.pas.batch.repository.PolicyRepository;
import com.acme.insurance.pas.batch.service.PremiumCalculationService;
import com.acme.insurance.pas.batch.writer.PremiumReportWriter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    private final PolicyRepository policyRepository;
    private final PremiumCalculationService calculationService;
    private final PremiumReportWriter reportWriter;

    @Value("${premium.chunk-size:100}")
    private int chunkSize;

    @Value("${premium.concurrency:4}")
    private int concurrency;

    public BatchConfig(PolicyRepository policyRepository,
                       PremiumCalculationService calculationService,
                       PremiumReportWriter reportWriter) {
        this.policyRepository = policyRepository;
        this.calculationService = calculationService;
        this.reportWriter = reportWriter;
    }

    @Bean
    public JdbcCursorItemReader<Policy> policyReader() {
        return new JdbcCursorItemReaderBuilder<Policy>()
                .name("policyReader")
                .dataSource(policyRepository.getDataSource())
                .sql(PolicyRepository.ACTIVE_POLICIES_SQL)
                .rowMapper(new PolicyRepository.PolicyRowMapper())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Policy> synchronizedPolicyReader(
            JdbcCursorItemReader<Policy> policyReader) {
        return new SynchronizedItemStreamReaderBuilder<Policy>()
                .delegate(policyReader)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<PremiumRecord> premiumWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PremiumRecord>()
                .dataSource(dataSource)
                .sql("INSERT INTO PREMIUMS "
                        + "(POLICY_NUMBER, COVERAGE_SEQ, "
                        + "TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE, "
                        + "BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR, "
                        + "EXPERIENCE_MOD, SCHEDULE_MOD, "
                        + "DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT, "
                        + "TOTAL_PREMIUM, INSTALLMENT_CODE, "
                        + "CALC_DATE, CALC_BY) "
                        + "VALUES "
                        + "(:policyNumber, :coverageSeq, "
                        + ":termEffDate, :termExpDate, "
                        + ":baseRate, :territoryFactor, :classFactor, "
                        + ":experienceMod, :scheduleMod, "
                        + ":discountPct, :surchargeAmt, :taxAmt, "
                        + ":totalPremium, :installmentCode, "
                        + ":calcDate, :calcBy)")
                .beanMapped()
                .build();
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("premium-batch-");
        executor.setConcurrencyLimit(concurrency);
        return executor;
    }

    @Bean
    public Step premiumCalculationStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       SynchronizedItemStreamReader<Policy> synchronizedPolicyReader,
                                       JdbcBatchItemWriter<PremiumRecord> premiumWriter,
                                       TaskExecutor batchTaskExecutor) {
        return new StepBuilder("premiumCalculationStep", jobRepository)
                .<Policy, PremiumRecord>chunk(chunkSize, transactionManager)
                .reader(synchronizedPolicyReader)
                .processor(calculationService::calculate)
                .writer(premiumWriter)
                .taskExecutor(batchTaskExecutor)
                .listener(new StepExecutionListenerSupport() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        reportWriter.writeSummary(stepExecution);
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    public Job premiumBatchJob(JobRepository jobRepository, Step premiumCalculationStep) {
        return new JobBuilder("premiumBatchJob", jobRepository)
                .start(premiumCalculationStep)
                .build();
    }
}
