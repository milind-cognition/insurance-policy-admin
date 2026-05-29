package com.acme.insurance.pas.controller;

import com.acme.insurance.pas.dto.PolicyCreationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void getPolicy_returnsPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-00000002"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.policy.policyNumber", is("POL-00000002")))
                .andExpect(jsonPath("$.policy.policyType", is("AUT")))
                .andExpect(jsonPath("$.policy.policyStatus", is("AC")));
    }

    @Test
    void getPolicy_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/policies/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCoverages_returnsCoverages() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-00000001/coverages"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].coverageType", is("DWEL")))
                .andExpect(jsonPath("$[1].coverageType", is("PERS")));
    }

    @Test
    void getCoverages_policyNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/policies/NONEXISTENT/coverages"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthCheck_responds() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    void createPolicy_validRequest_returns201() throws Exception {
        PolicyCreationRequest request = new PolicyCreationRequest();
        request.setPolicyType("AUT");
        request.setEffectiveDate(LocalDate.now().plusDays(1));
        request.setPolicyholderId("C000000001");
        request.setCoverageLimit(new BigDecimal("100000.00"));
        request.setBranchCode("CHI1");

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyNumber", startsWith("POL")))
                .andExpect(jsonPath("$.policyStatus", is("PN")))
                .andExpect(jsonPath("$.policyType", is("AUT")));
    }

    @Test
    void createPolicy_invalidRequest_returns400() throws Exception {
        PolicyCreationRequest request = new PolicyCreationRequest();

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renewPolicy_validPolicy_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/policies/POL-00000001/renewals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.renewalCount", greaterThan(0)));
    }

    @Test
    void renewPolicy_notFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/policies/NONEXISTENT/renewals"))
                .andExpect(status().isNotFound());
    }

    @Test
    void underwriting_validPolicy_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/policies/POL-00000001/underwriting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.decisionCode", notNullValue()))
                .andExpect(jsonPath("$.riskScore", notNullValue()));
    }
}
