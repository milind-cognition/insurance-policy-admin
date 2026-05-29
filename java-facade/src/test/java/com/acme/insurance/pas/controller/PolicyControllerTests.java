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
    public void createPolicy_returnsCreated() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"C000000001\"," +
                "\"policyType\":\"HOM\"," +
                "\"effectiveDate\":\"2026-07-01\"," +
                "\"expiryDate\":\"2027-07-01\"," +
                "\"agentCode\":\"AG1001\"," +
                "\"branchCode\":\"CHI1\"," +
                "\"totalPremium\":1500.00," +
                "\"deductible\":1000.00," +
                "\"coverageLimit\":600000.00" +
                "}";
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyNumber", startsWith("POL")))
                .andExpect(jsonPath("$.policyType", is("HOM")))
                .andExpect(jsonPath("$.policyStatus", is("PN")))
                .andExpect(jsonPath("$.uwStatus", is("PN")))
                .andExpect(jsonPath("$.riskScore", is(0)));
    }

    @Test
    public void createPolicy_missingPolicyholderId_returns400() throws Exception {
        String json = "{" +
                "\"policyType\":\"HOM\"," +
                "\"effectiveDate\":\"2026-07-01\"," +
                "\"expiryDate\":\"2027-07-01\"," +
                "\"coverageLimit\":600000.00" +
                "}";
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error",
                        is("POLICYHOLDER ID IS REQUIRED")));
    }

    @Test
    public void createPolicy_missingPolicyType_returns400() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"C000000001\"," +
                "\"effectiveDate\":\"2026-07-01\"," +
                "\"expiryDate\":\"2027-07-01\"," +
                "\"coverageLimit\":600000.00" +
                "}";
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error",
                        is("POLICY TYPE IS REQUIRED")));
    }

    @Test
    public void createPolicy_invalidPolicyholder_returns400() throws Exception {
        String json = "{" +
                "\"policyholderId\":\"INVALID999\"," +
                "\"policyType\":\"AUT\"," +
                "\"effectiveDate\":\"2026-07-01\"," +
                "\"expiryDate\":\"2027-07-01\"," +
                "\"coverageLimit\":100000.00" +
                "}";
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error",
                        is("POLICYHOLDER NOT FOUND IN SYSTEM")));
    }
}
