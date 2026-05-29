package com.acme.insurance.pas.controller;

import com.acme.insurance.pas.model.PolicyHolder;
import com.acme.insurance.pas.repository.PolicyHolderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policyholders")
public class PolicyHolderController {

    private final PolicyHolderRepository policyHolderRepository;

    public PolicyHolderController(PolicyHolderRepository policyHolderRepository) {
        this.policyHolderRepository = policyHolderRepository;
    }

    @GetMapping("/{custId}")
    public ResponseEntity<PolicyHolder> getPolicyHolder(@PathVariable String custId) {
        return policyHolderRepository.findById(custId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
