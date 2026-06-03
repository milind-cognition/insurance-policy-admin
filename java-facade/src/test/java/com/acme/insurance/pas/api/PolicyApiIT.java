package com.acme.insurance.pas.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * REST API integration suite for the PAS facade, driven by TestNG + REST Assured.
 *
 * These run against the live HTTP server (booted by the Spring Boot Maven plugin
 * during `mvn verify`, H2 'local' profile). Override the target with
 * -Dpas.baseUri=http://host:port to point at any environment.
 *
 * The facade is a read-only window onto the COBOL/DB2 mainframe policy data, so
 * this exercises the mainframe-connected REST contract end to end.
 */
public class PolicyApiIT {

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = System.getProperty("pas.baseUri", "http://localhost:8080");
    }

    @Test(description = "Actuator health endpoint reports UP")
    public void healthIsUp() {
        given()
        .when()
            .get("/manage/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test(description = "Policy lookup returns the full policy record")
    public void getPolicyReturnsRecord() {
        given()
        .when()
            .get("/api/v1/policies/POL-00000001")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("policyNumber", equalTo("POL-00000001"))
            .body("policyType", equalTo("HOM"))
            .body("policyStatus", equalTo("AC"))
            .body("totalPremium", equalTo(1250.0f))
            .body("policyholderId", equalTo("C000000001"));
    }

    @Test(description = "Coverage lookup returns all coverages for a policy")
    public void getCoveragesReturnsList() {
        given()
        .when()
            .get("/api/v1/policies/POL-00000001/coverages")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(2))
            .body("coverageType", equalTo(java.util.Arrays.asList("DWEL", "PERS")))
            .body("[0].premium", equalTo(850.0f));
    }

    @Test(description = "Unknown policy returns 404")
    public void unknownPolicyReturns404() {
        given()
        .when()
            .get("/api/v1/policies/POL-DOES-NOT-EXIST")
        .then()
            .statusCode(404);
    }

    @Test(description = "Coverages for an unknown policy return 404")
    public void coveragesForUnknownPolicyReturns404() {
        given()
        .when()
            .get("/api/v1/policies/POL-DOES-NOT-EXIST/coverages")
        .then()
            .statusCode(404);
    }

    @DataProvider(name = "seededPolicies")
    public Object[][] seededPolicies() {
        return new Object[][] {
            { "POL-00000001", "HOM" },
            { "POL-00000002", "AUT" },
            { "POL-00000003", "CGL" },
        };
    }

    @Test(dataProvider = "seededPolicies",
          description = "Each seeded mainframe policy is reachable with the right line of business")
    public void seededPoliciesAreReachable(String policyNumber, String expectedType) {
        given()
        .when()
            .get("/api/v1/policies/" + policyNumber)
        .then()
            .statusCode(200)
            .body("policyNumber", equalTo(policyNumber))
            .body("policyType", equalTo(expectedType))
            .body("effectiveDate", notNullValue());
    }
}
