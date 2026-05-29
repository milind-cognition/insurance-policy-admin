package com.acme.insurance.pas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAS Premium Engine Application.
 *
 * Java 21 Spring Boot 3.x services replacing the COBOL PREMBAT premium
 * calculation batch program and UNDWRT underwriting decision program.
 * Includes a Spring Batch job that replaces the JCL PREMIUM-BATCH flow.
 */
@SpringBootApplication
public class PremiumEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PremiumEngineApplication.class, args);
    }
}
