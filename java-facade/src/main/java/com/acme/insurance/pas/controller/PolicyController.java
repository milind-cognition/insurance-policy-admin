package com.acme.insurance.pas.controller;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.EndorsementRequest;
import com.acme.insurance.pas.model.EndorsementResponse;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.PolicyRepository;
import com.acme.insurance.pas.service.PolicyEndorsementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Policy Controller - Read-only REST endpoints for policy data.
 *
 * This controller exposes policy data from the DB2 mainframe database
 * via REST/JSON.
 *
 * Endpoints:
 *   GET  /api/v1/policies/{policyNumber}               - Policy details
 *   GET  /api/v1/policies/{policyNumber}/coverages      - Coverage details
 *   POST /api/v1/policies/{policyNumber}/endorsements   - Process endorsement
 *
 * NOTE: No authentication on these endpoints - relies on network
 * segmentation (internal VPN only). TODO: Add OAuth2 in Phase 2.
 *
 * @author T. Nguyen (2022)
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyEndorsementService endorsementService;

    @GetMapping("/{policyNumber}")
    public ResponseEntity<Policy> getPolicy(@PathVariable String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return new ResponseEntity<Policy>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Policy>(policy, HttpStatus.OK);
    }

    @GetMapping("/{policyNumber}/coverages")
    public ResponseEntity<List<Coverage>> getCoverages(
            @PathVariable String policyNumber) {
        // First verify the policy exists
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            return new ResponseEntity<List<Coverage>>(HttpStatus.NOT_FOUND);
        }
        List<Coverage> coverages = policyRepository.findCoveragesByPolicyNumber(
                policyNumber);
        return new ResponseEntity<List<Coverage>>(coverages, HttpStatus.OK);
    }

    @PostMapping("/{policyNumber}/endorsements")
    public ResponseEntity<?> processEndorsement(
            @PathVariable String policyNumber,
            @RequestBody EndorsementRequest request) {
        try {
            EndorsementResponse response =
                    endorsementService.processEndorsement(
                            policyNumber, request);
            return new ResponseEntity<EndorsementResponse>(
                    response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
