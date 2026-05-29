package com.acme.insurance.pas.controller;

import com.acme.insurance.pas.dto.EndorsementRequest;
import com.acme.insurance.pas.dto.PolicyCreationRequest;
import com.acme.insurance.pas.dto.PolicyDetailResponse;
import com.acme.insurance.pas.dto.UnderwritingResult;
import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.service.EndorsementService;
import com.acme.insurance.pas.service.PolicyCreationService;
import com.acme.insurance.pas.service.PolicyInquiryService;
import com.acme.insurance.pas.service.RenewalService;
import com.acme.insurance.pas.service.UnderwritingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyInquiryService policyInquiryService;
    private final PolicyCreationService policyCreationService;
    private final EndorsementService endorsementService;
    private final RenewalService renewalService;
    private final UnderwritingService underwritingService;

    public PolicyController(PolicyInquiryService policyInquiryService,
                            PolicyCreationService policyCreationService,
                            EndorsementService endorsementService,
                            RenewalService renewalService,
                            UnderwritingService underwritingService) {
        this.policyInquiryService = policyInquiryService;
        this.policyCreationService = policyCreationService;
        this.endorsementService = endorsementService;
        this.renewalService = renewalService;
        this.underwritingService = underwritingService;
    }

    @GetMapping("/{policyNumber}")
    public ResponseEntity<PolicyDetailResponse> getPolicy(
            @PathVariable String policyNumber) {
        PolicyDetailResponse response = policyInquiryService.inquirePolicy(policyNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{policyNumber}/coverages")
    public ResponseEntity<List<Coverage>> getCoverages(
            @PathVariable String policyNumber) {
        PolicyDetailResponse response = policyInquiryService.inquirePolicy(policyNumber);
        return ResponseEntity.ok(response.getCoverages());
    }

    @PostMapping
    public ResponseEntity<Policy> createPolicy(
            @Valid @RequestBody PolicyCreationRequest request) {
        Policy policy = policyCreationService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(policy);
    }

    @PostMapping("/{policyNumber}/endorsements")
    public ResponseEntity<Endorsement> processEndorsement(
            @PathVariable String policyNumber,
            @Valid @RequestBody EndorsementRequest request) {
        request.setPolicyNumber(policyNumber);
        Endorsement endorsement = endorsementService.processEndorsement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(endorsement);
    }

    @PostMapping("/{policyNumber}/renewals")
    public ResponseEntity<Policy> renewPolicy(@PathVariable String policyNumber) {
        Policy policy = renewalService.renewPolicy(policyNumber);
        return ResponseEntity.ok(policy);
    }

    @PostMapping("/{policyNumber}/underwriting")
    public ResponseEntity<UnderwritingResult> evaluateRisk(
            @PathVariable String policyNumber) {
        UnderwritingResult result = underwritingService.evaluateRisk(policyNumber);
        return ResponseEntity.ok(result);
    }
}
