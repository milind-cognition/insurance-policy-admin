package com.acme.insurance.pas.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PolicyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getPolicy_returnsPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-00000001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.policyType", is("HOM")))
                .andExpect(jsonPath("$.policyStatus", is("AC")))
                .andExpect(jsonPath("$.totalPremium", is(1250.0)));
    }

    @Test
    public void getPolicy_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/policies/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getCoverages_returnsCoverages() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-00000001/coverages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].coverageType", is("DWEL")))
                .andExpect(jsonPath("$[1].coverageType", is("PERS")));
    }

    @Test
    public void getCoverages_policyNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/policies/NONEXISTENT/coverages"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void healthCheck_returnsUp() throws Exception {
        mockMvc.perform(get("/manage/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    public void createPolicy_success() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"C000000001\"," +
                "\"policyType\":\"AUT\"," +
                "\"effectiveDate\":\"2027-01-01\"," +
                "\"expiryDate\":\"2028-01-01\"," +
                "\"agentCode\":\"AG1001\"," +
                "\"branchCode\":\"CHI1\"," +
                "\"totalPremium\":950.00," +
                "\"deductible\":500.00," +
                "\"coverageLimit\":100000.00" +
                "}";

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyNumber", startsWith("POL-")))
                .andExpect(jsonPath("$.policyStatus", is("PN")))
                .andExpect(jsonPath("$.coveragesCreated", is(2)));
    }

    @Test
    public void createPolicy_missingPolicyholderId() throws Exception {
        String json = "{" +
                "\"policyType\":\"AUT\"," +
                "\"effectiveDate\":\"2027-01-01\"," +
                "\"coverageLimit\":100000.00" +
                "}";

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("POLICYHOLDER ID IS REQUIRED")));
    }

    @Test
    public void createPolicy_invalidPolicyholderNotInDb() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"CNOTEXIST1\"," +
                "\"policyType\":\"HOM\"," +
                "\"effectiveDate\":\"2027-01-01\"," +
                "\"coverageLimit\":500000.00" +
                "}";

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("POLICYHOLDER NOT FOUND IN SYSTEM")));
    }

    @Test
    public void createPolicy_missingPolicyType() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"C000000001\"," +
                "\"effectiveDate\":\"2027-01-01\"," +
                "\"coverageLimit\":100000.00" +
                "}";

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("POLICY TYPE IS REQUIRED")));
    }
}
