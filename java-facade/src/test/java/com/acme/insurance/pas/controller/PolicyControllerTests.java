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

    // --- Renewal endpoint tests (POLRNW migration) ---

    @Test
    public void renewPolicy_happyPath() throws Exception {
        // POL-00000001: AC status, premium 1250.00, expiry 2026-01-01, renewalCount 5
        mockMvc.perform(post("/api/v1/policies/POL-00000001/renewal"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.previousPremium", is(1250.0)))
                .andExpect(jsonPath("$.newPremium", is(1312.5)))
                .andExpect(jsonPath("$.rateChangePct", is(5.0)))
                .andExpect(jsonPath("$.rateCapped", is(false)))
                .andExpect(jsonPath("$.renewalCount", is(6)))
                .andExpect(jsonPath("$.newEffectiveDate", notNullValue()))
                .andExpect(jsonPath("$.newExpiryDate", notNullValue()));
    }

    @Test
    public void renewPolicy_notFound() throws Exception {
        mockMvc.perform(post("/api/v1/policies/NONEXISTENT/renewal"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void renewPolicy_notEligible_cancelledPolicy() throws Exception {
        // POL-00000004: CN (cancelled) status - should be rejected
        mockMvc.perform(post("/api/v1/policies/POL-00000004/renewal"))
                .andExpect(status().isBadRequest());
    }
}
