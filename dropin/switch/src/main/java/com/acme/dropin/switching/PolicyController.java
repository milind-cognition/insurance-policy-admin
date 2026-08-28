package com.acme.dropin.switching;

import com.acme.dropin.contract.ContractXml;
import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.PasBackend;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The caller-facing surface, identical in shape to the incumbent facade's
 * controller: same paths, same verbs, same 404 with an empty body.
 *
 * <p>XML variants are served on the same paths for the CICS Web Services style
 * of caller. The renewal endpoint has no equivalent in the incumbent REST
 * facade - today PRWL/POLRNW is reached through CICS - so it is additive, and
 * marked as such in the contract.
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PasBackend backend;

    public PolicyController(PasBackend backend) {
        this.backend = backend;
    }

    @GetMapping(value = "/{policyNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicyView> getPolicy(@PathVariable String policyNumber) {
        return backend.findPolicy(policyNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{policyNumber}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getPolicyAsXml(@PathVariable String policyNumber) {
        return backend.findPolicy(policyNumber)
                .map(policy -> ResponseEntity.ok(ContractXml.write(policy)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{policyNumber}/coverages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoverageView>> getCoverages(@PathVariable String policyNumber) {
        if (backend.findPolicy(policyNumber).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(backend.findCoverages(policyNumber));
    }

    @GetMapping(value = "/{policyNumber}/coverages", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getCoveragesAsXml(@PathVariable String policyNumber) {
        if (backend.findPolicy(policyNumber).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ContractXml.writeCoverages(backend.findCoverages(policyNumber)));
    }

    /** PRWL / POLRNW, reachable over the same service boundary. */
    @PostMapping(value = "/{policyNumber}/renewals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> renew(@PathVariable String policyNumber) {
        RenewalResult result = backend.renew(policyNumber);
        if (result.succeeded()) {
            return ResponseEntity.ok(result.policy());
        }
        if (RenewalResult.NOT_FOUND.equals(result.errorMessage())) {
            return ResponseEntity.status(404).body(Map.of("error", result.errorMessage()));
        }
        return ResponseEntity.status(409).body(Map.of("error", result.errorMessage()));
    }
}
