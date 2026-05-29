package com.acme.insurance.pas.batch;

import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.CoverageRepository;
import com.acme.insurance.pas.repository.PolicyRepository;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class DailyExtractJob {

    private static final Logger log = LoggerFactory.getLogger(DailyExtractJob.class);

    private final PolicyRepository policyRepository;
    private final CoverageRepository coverageRepository;

    public DailyExtractJob(PolicyRepository policyRepository,
                           CoverageRepository coverageRepository) {
        this.policyRepository = policyRepository;
        this.coverageRepository = coverageRepository;
    }

    @Bean
    public Job dailyPolicyExtractJob(JobRepository jobRepository, Step dailyExtractStepDef) {
        return new JobBuilder("dailyExtractJob", jobRepository)
                .start(dailyExtractStepDef)
                .build();
    }

    @Bean
    public Step dailyExtractStepDef(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Path outputDir = Paths.get("extract");
            Files.createDirectories(outputDir);

            List<Policy> activePolicies = policyRepository.findByPolicyStatus("AC");
            List<Policy> pendingPolicies = policyRepository.findByPolicyStatus("PN");
            activePolicies.addAll(pendingPolicies);

            Path policyFile = outputDir.resolve("policy_extract_" + dateStr + ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(policyFile)) {
                writer.write("POLICY_NUMBER,POLICY_TYPE,POLICY_STATUS,EFFECTIVE_DATE," +
                        "EXPIRY_DATE,POLICYHOLDER_ID,TOTAL_PREMIUM,UW_STATUS,RISK_SCORE");
                writer.newLine();
                for (Policy p : activePolicies) {
                    writer.write(String.join(",",
                            p.getPolicyNumber(),
                            p.getPolicyType(),
                            p.getPolicyStatus(),
                            p.getEffectiveDate().toString(),
                            p.getExpiryDate().toString(),
                            p.getPolicyholderId(),
                            p.getTotalPremium().toPlainString(),
                            p.getUwStatus(),
                            String.valueOf(p.getRiskScore())));
                    writer.newLine();
                }
            }

            log.info("Daily extract complete: {} policies written to {}",
                    activePolicies.size(), policyFile);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("dailyExtractStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
