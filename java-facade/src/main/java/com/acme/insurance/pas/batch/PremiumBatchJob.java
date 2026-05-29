package com.acme.insurance.pas.batch;

import com.acme.insurance.pas.dto.PremiumBatchSummary;
import com.acme.insurance.pas.service.PremiumCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PremiumBatchJob {

    private static final Logger log = LoggerFactory.getLogger(PremiumBatchJob.class);

    private final PremiumCalculationService premiumCalculationService;

    public PremiumBatchJob(PremiumCalculationService premiumCalculationService) {
        this.premiumCalculationService = premiumCalculationService;
    }

    @Bean
    public Job premiumCalculationJob(JobRepository jobRepository, Step premiumCalcStep) {
        return new JobBuilder("premiumCalculationJob", jobRepository)
                .start(premiumCalcStep)
                .build();
    }

    @Bean
    public Step premiumCalcStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            PremiumBatchSummary summary = premiumCalculationService.calculateAllPremiums();
            log.info("Premium batch complete: read={}, updated={}, errors={}",
                    summary.getPoliciesRead(),
                    summary.getPoliciesUpdated(),
                    summary.getPoliciesError());
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("premiumStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
