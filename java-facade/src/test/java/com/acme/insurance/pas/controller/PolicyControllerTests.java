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

    // --- Underwriting endpoint tests (UNDWRT migration) ---

    @Test
    public void underwriting_happyPath_returnsDecision() throws Exception {
        mockMvc.perform(post("/api/v1/policies/POL-00000001/underwriting"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.decisionCode", isIn(
                        new String[]{"AP", "RS", "RM", "DC"})))
                .andExpect(jsonPath("$.riskScore", greaterThan(0)))
                .andExpect(jsonPath("$.decisionReason", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.claimCount", is(4)));
    }

    @Test
    public void underwriting_notFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/policies/NONEXISTENT/underwriting"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void underwriting_riskScoreMatchesSeedData() throws Exception {
        // POL-00000001: credit=720, tier=A, 4 claims, high loss ratio
        // Base: (900-720)/2 = 90
        // Claims >3: +150
        // Loss ratio (50500/1250)*100 = 4040 > 80: +200
        // Expected total: 440 -> RS (Referred to Senior UW)
        mockMvc.perform(post("/api/v1/policies/POL-00000001/underwriting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", is(440)))
                .andExpect(jsonPath("$.decisionCode", is("RS")))
                .andExpect(jsonPath("$.decisionReason",
                        is("REFERRED TO SENIOR UNDERWRITER")));
    }

    @Test
    public void underwriting_policyTwoScoreCorrect() throws Exception {
        // POL-00000002: credit=680, tier=B, 1 claim, loss ratio 359 > 80
        // Base: (900-680)/2 = 110
        // Loss ratio: +200
        // Expected total: 310 -> RS
        mockMvc.perform(post("/api/v1/policies/POL-00000002/underwriting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", is(310)))
                .andExpect(jsonPath("$.decisionCode", is("RS")));
    }
}
