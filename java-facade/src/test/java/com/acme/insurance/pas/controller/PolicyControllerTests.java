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
    public void inquirePolicy_returnsFullInquiry() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-00000001/inquiry"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.policy.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.policy.policyType", is("HOM")))
                .andExpect(jsonPath("$.policy.policyStatus", is("AC")))
                .andExpect(jsonPath("$.policy.totalPremium", is(1250.0)))
                .andExpect(jsonPath("$.customer.custId", is("C000000001")))
                .andExpect(jsonPath("$.customer.lastName", is("Smith")))
                .andExpect(jsonPath("$.customer.firstName", is("John")))
                .andExpect(jsonPath("$.customer.email", is("john.smith@example.com")))
                .andExpect(jsonPath("$.coverages", hasSize(2)))
                .andExpect(jsonPath("$.coverages[0].coverageType", is("DWEL")))
                .andExpect(jsonPath("$.coverages[1].coverageType", is("PERS")));
    }

    @Test
    public void inquirePolicy_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/policies/NONEXISTENT/inquiry"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void inquirePolicy_blankPolicyNumber_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/policies/ /inquiry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("POLICY NUMBER IS REQUIRED")));
    }

    @Test
    public void healthCheck_returnsUp() throws Exception {
        mockMvc.perform(get("/manage/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
