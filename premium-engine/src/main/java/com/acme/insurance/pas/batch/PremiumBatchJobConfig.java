package com.acme.insurance.pas.batch;

import com.acme.insurance.pas.entity.Policy;
import com.acme.insurance.pas.service.PremiumCalculationService;
import com.acme.insurance.pas.service.PremiumCalculationService.PremiumResult;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration replacing the JCL PREMIUM-BATCH.jcl flow.
 *
 * Original JCL flow:
 *   STEP010 - Execute PREMBAT COBOL program against all active policies
 *   STEP020 - Check return code (RC=0 → success)
 *   STEP030 - If RC>4, send warning notification
 *
 * This Spring Batch job replaces that with:
 *   Step 1 (calculatePremiums) - Read active policies via JPA, calculate
 *           premiums using PremiumCalculationService, persist results.
 */
@Configuration
public class PremiumBatchJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PremiumBatchJobConfig.class);
    private static final int CHUNK_SIZE = 50;

    private final PremiumCalculationService premiumCalculationService;
    private final EntityManagerFactory entityManagerFactory;

    public PremiumBatchJobConfig(PremiumCalculationService premiumCalculationService,
                                 EntityManagerFactory entityManagerFactory) {
        this.premiumCalculationService = premiumCalculationService;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Bean
    public JpaPagingItemReader<Policy> activePolicyReader() {
        return new JpaPagingItemReaderBuilder<Policy>()
                .name("activePolicyReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Policy p WHERE p.policyStatus = 'AC' " +
                             "ORDER BY p.policyNumber")
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<Policy, PremiumResult> premiumProcessor() {
        return policy -> {
            try {
                return premiumCalculationService.calculatePremium(policy);
            } catch (Exception e) {
                log.error("Error calculating premium for {}: {}",
                        policy.getPolicyNumber(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<PremiumResult> premiumWriter() {
        return chunk -> {
            for (PremiumResult result : chunk.getItems()) {
                Policy policy = new Policy();
                policy.setPolicyNumber(result.policyNumber());
                premiumCalculationService.persistPremiumResult(policy, result);
                log.info("{} PREMIUM: {}", result.policyNumber(), result.finalPremium());
            }
        };
    }

    @Bean
    public Step calculatePremiumsStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("calculatePremiums", jobRepository)
                .<Policy, PremiumResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(activePolicyReader())
                .processor(premiumProcessor())
                .writer(premiumWriter())
                .build();
    }

    @Bean
    public Job premiumBatchJob(JobRepository jobRepository, Step calculatePremiumsStep) {
        return new JobBuilder("premiumBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(calculatePremiumsStep)
                .build();
    }
}
