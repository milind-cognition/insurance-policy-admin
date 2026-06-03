package com.acme.insurance.pas.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Step definitions backing the policy-inquiry feature. Each step issues a real
 * HTTP call to the running facade (the read-only window onto the COBOL/DB2
 * mainframe), so these scenarios are true functional tests of the API contract.
 *
 * Override the target with -Dpas.baseUri=http://host:port.
 */
public class PolicySteps {

    private String baseUri;
    private Response response;

    @Given("the PAS facade is available")
    public void theFacadeIsAvailable() {
        baseUri = System.getProperty("pas.baseUri", "http://localhost:8080");
        RestAssured.baseURI = baseUri;
    }

    @When("I request policy {string}")
    public void iRequestPolicy(String policyNumber) {
        response = RestAssured.given().get("/api/v1/policies/" + policyNumber);
    }

    @When("I request the coverages for policy {string}")
    public void iRequestCoverages(String policyNumber) {
        response = RestAssured.given()
                .get("/api/v1/policies/" + policyNumber + "/coverages");
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expected) {
        assertEquals(response.getStatusCode(), expected);
    }

    @Then("the policy type is {string}")
    public void thePolicyTypeIs(String expected) {
        assertEquals(response.jsonPath().getString("policyType"), expected);
    }

    @Then("the policy status is {string}")
    public void thePolicyStatusIs(String expected) {
        assertEquals(response.jsonPath().getString("policyStatus"), expected);
    }

    @Then("the total premium is {double}")
    public void theTotalPremiumIs(double expected) {
        assertEquals(response.jsonPath().getDouble("totalPremium"), expected);
    }

    @Then("there are {int} coverages")
    public void thereAreNCoverages(int expected) {
        List<?> coverages = response.jsonPath().getList("$");
        assertEquals(coverages.size(), expected);
    }

    @Then("the coverage types are {string}")
    public void theCoverageTypesAre(String csv) {
        List<String> expected = new ArrayList<String>();
        for (String s : csv.split(",")) {
            expected.add(s.trim());
        }
        List<String> actual = response.jsonPath().getList("coverageType");
        assertEquals(actual, expected);
    }
}
