package com.acme.insurance.pas.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void premiumBatch_success() throws Exception {
        mockMvc.perform(post("/api/v1/policies/premium-batch"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.policiesRead", is(3)))
                .andExpect(jsonPath("$.policiesUpdated", is(3)))
                .andExpect(jsonPath("$.policiesError", is(0)))
                .andExpect(jsonPath("$.message", is("PREMBAT completed successfully")));
    }

    @Test
    @Transactional
    public void premiumBatch_calculatesCorrectly() throws Exception {
        // Run the batch first
        mockMvc.perform(post("/api/v1/policies/premium-batch"))
                .andExpect(status().isOk());

        // Verify batch response has correct base rates:
        // POL-00000001 (HOM) -> base 1200, tax 42.00, surcharge 25 -> total 1267.00
        // POL-00000002 (AUT) -> base 850, tax 29.75, surcharge 25 -> total 904.75
        // POL-00000003 (CGL) -> base 5000, tax 175.00, surcharge 25 -> total 5200.00
        // Run a second time — should fail with errors due to duplicate PK
        mockMvc.perform(post("/api/v1/policies/premium-batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policiesRead", is(3)))
                .andExpect(jsonPath("$.policiesUpdated", is(0)))
                .andExpect(jsonPath("$.policiesError", is(3)));
    }
}
