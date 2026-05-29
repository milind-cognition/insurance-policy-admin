package com.acme.insurance.pas.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point for the premium batch calculation job.
 * Replaces the COBOL PREMBAT program and PREMIUM-BATCH.jcl job.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PremiumBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
                SpringApplication.run(PremiumBatchApplication.class, args)));
    }
}
