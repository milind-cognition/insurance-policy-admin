package com.acme.insurance.pas.bdd;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Cucumber-JVM runner (TestNG engine). Named *IT so Failsafe executes it during
 * `mvn verify`, while the Spring Boot Maven plugin keeps the facade running on the
 * H2 'local' profile.
 */
@CucumberOptions(
        features = "classpath:features",
        glue = "com.acme.insurance.pas.bdd",
        plugin = {"pretty", "summary"}
)
public class RunCucumberIT extends AbstractTestNGCucumberTests {
}
