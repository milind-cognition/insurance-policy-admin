package com.acme.dropin.switching;

import com.acme.dropin.contract.PasBackend;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostics, deliberately outside {@code /api/v1}: it tells the operator
 * which implementation is answering. Callers neither see nor need it - that is
 * the whole point.
 */
@RestController
public class BackendInfoController {

    private final PasBackend backend;

    public BackendInfoController(PasBackend backend) {
        this.backend = backend;
    }

    @GetMapping(value = "/manage/backend", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> backend() {
        return Map.of("backend", backend.name());
    }
}
