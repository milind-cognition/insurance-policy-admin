package com.acme.insurance.pas.batch;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class MonthlyExposureJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyExposureJob.class);

    private final EntityManager entityManager;

    public MonthlyExposureJob(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Bean
    public Job monthlyExposureExportJob(JobRepository jobRepository, Step monthlyExposureStepDef) {
        return new JobBuilder("monthlyExposureJob", jobRepository)
                .start(monthlyExposureStepDef)
                .build();
    }

    @Bean
    public Step monthlyExposureStepDef(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Path outputDir = Paths.get("extract");
            Files.createDirectories(outputDir);

            Query query = entityManager.createNativeQuery(
                    "SELECT p.POLICY_TYPE, p.BRANCH_CODE, c.COVERAGE_TYPE, " +
                    "c.RATING_TERRITORY, c.CLASS_CODE, " +
                    "COUNT(*) AS POLICY_COUNT, " +
                    "SUM(c.COVERAGE_LIMIT) AS TOTAL_EXPOSURE, " +
                    "SUM(c.PREMIUM) AS TOTAL_PREMIUM, " +
                    "SUM(c.DEDUCTIBLE) AS TOTAL_DEDUCTIBLE " +
                    "FROM ACMEINS.POLICIES p " +
                    "JOIN ACMEINS.COVERAGES c ON p.POLICY_NUMBER = c.POLICY_NUMBER " +
                    "WHERE p.POLICY_STATUS = 'AC' AND c.STATUS = 'AC' " +
                    "GROUP BY p.POLICY_TYPE, p.BRANCH_CODE, " +
                    "c.COVERAGE_TYPE, c.RATING_TERRITORY, c.CLASS_CODE " +
                    "ORDER BY p.POLICY_TYPE, p.BRANCH_CODE");

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            Path outputFile = outputDir.resolve("exposure_monthly_" + dateStr + ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                writer.write("POLICY_TYPE,BRANCH_CODE,COVERAGE_TYPE,RATING_TERRITORY," +
                        "CLASS_CODE,POLICY_COUNT,TOTAL_EXPOSURE,TOTAL_PREMIUM,TOTAL_DEDUCTIBLE");
                writer.newLine();
                for (Object[] row : results) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(row[i] != null ? row[i].toString().trim() : "");
                    }
                    writer.write(sb.toString());
                    writer.newLine();
                }
            }

            log.info("Monthly exposure extract complete: {} rows written to {}",
                    results.size(), outputFile);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("monthlyExposureStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
