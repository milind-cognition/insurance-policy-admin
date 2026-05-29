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

    // ---- POLEND endorsement tests ----

    @Test
    public void processEndorsement_success() throws Exception {
        String json = "{\"endorsementType\":\"CAD\"," +
                "\"description\":\"Add flood coverage\"," +
                "\"premiumAdjustment\":150.00}";

        mockMvc.perform(post("/api/v1/policies/POL-00000001/endorsements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyNumber", is("POL-00000001")))
                .andExpect(jsonPath("$.endorsementSeq", is(1)))
                .andExpect(jsonPath("$.endorsementType", is("CAD")))
                .andExpect(jsonPath("$.premiumAdjustment", is(150.0)))
                .andExpect(jsonPath("$.newTotalPremium", is(1400.0)))
                .andExpect(jsonPath("$.prorataFactor").isNotEmpty())
                .andExpect(jsonPath("$.message",
                        is("ENDORSEMENT PROCESSED SUCCESSFULLY")));
    }

    @Test
    public void processEndorsement_policyNotFound() throws Exception {
        String json = "{\"endorsementType\":\"CAD\"," +
                "\"description\":\"Test\"," +
                "\"premiumAdjustment\":100.00}";

        mockMvc.perform(post("/api/v1/policies/NONEXISTENT/endorsements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void processEndorsement_notActivePolicy() throws Exception {
        String json = "{\"endorsementType\":\"CAD\"," +
                "\"description\":\"Test\"," +
                "\"premiumAdjustment\":100.00}";

        mockMvc.perform(post("/api/v1/policies/POL-00000099/endorsements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void processEndorsement_missingType() throws Exception {
        String json = "{\"description\":\"Test\"," +
                "\"premiumAdjustment\":100.00}";

        mockMvc.perform(post("/api/v1/policies/POL-00000001/endorsements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
