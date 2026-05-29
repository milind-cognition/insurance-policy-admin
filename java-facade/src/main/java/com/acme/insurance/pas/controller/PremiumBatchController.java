package com.acme.insurance.pas.controller;

import com.acme.insurance.pas.dto.PremiumBatchSummary;
import com.acme.insurance.pas.service.PremiumCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch")
public class PremiumBatchController {

    private final PremiumCalculationService premiumCalculationService;

    public PremiumBatchController(PremiumCalculationService premiumCalculationService) {
        this.premiumCalculationService = premiumCalculationService;
    }

    @PostMapping("/premiums")
    public ResponseEntity<PremiumBatchSummary> runPremiumBatch() {
        PremiumBatchSummary summary = premiumCalculationService.calculateAllPremiums();
        return ResponseEntity.ok(summary);
    }
}
