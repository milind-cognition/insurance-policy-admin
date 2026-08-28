package com.acme.dropin.switching;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claim under test: one caller, unchanged, against two implementations.
 *
 * <p>Each backend is started as the real service on a real port and driven over
 * HTTP by the same request code. Inquiry responses have to match byte for byte.
 * Renewal is deliberately not asserted here - {@code dropin/parity} is where
 * the two are compared across the whole population, and where they part company.
 */
class BackendSwitchTest {

    private static <T> T against(String backend, Function<RestClient, T> caller) {
        try (ConfigurableApplicationContext context = start(backend)) {
            int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
            return caller.apply(RestClient.create("http://localhost:" + port));
        }
    }

    private static ConfigurableApplicationContext start(String backend) {
        SpringApplication application = new SpringApplicationBuilder(SwitchApplication.class).build();
        return application.run("--pas.backend=" + backend, "--server.port=0",
                "--pas.mirror.url=jdbc:h2:mem:switchtest;DB_CLOSE_DELAY=-1;MODE=DB2");
    }

    private static Function<RestClient, String> get(String path) {
        return client -> client.get().uri(path).retrieve().body(String.class);
    }

    @Test
    void thePolicyDocumentIsIdenticalWhicheverBackendAnswers() {
        String path = "/api/v1/policies/PAS-00000042";
        assertEquals(against("mainframe", get(path)), against("mirror", get(path)));
    }

    @Test
    void theCoverageDocumentIsIdenticalWhicheverBackendAnswers() {
        String path = "/api/v1/policies/PAS-00000042/coverages";
        String json = against("mainframe", get(path));
        assertTrue(json.startsWith("[{"), "an array of coverage objects");
        assertEquals(json, against("mirror", get(path)));
    }

    @Test
    void theXmlProjectionIsIdenticalWhicheverBackendAnswers() {
        Function<RestClient, String> xml = client -> client.get()
                .uri("/api/v1/policies/PAS-00000042")
                .accept(org.springframework.http.MediaType.APPLICATION_XML)
                .retrieve().body(String.class);
        String legacy = against("mainframe", xml);
        assertTrue(legacy.startsWith("<policy>"));
        assertEquals(legacy, against("mirror", xml));
    }

    @Test
    void anUnknownPolicyIs404OnBothBackends() {
        Function<RestClient, HttpStatusCode> status = client -> client.get()
                .uri("/api/v1/policies/PAS-99999999")
                .retrieve()
                .onStatus(code -> true, (request, response) -> { })
                .toBodilessEntity()
                .getStatusCode();
        assertEquals(404, against("mainframe", status).value());
        assertEquals(404, against("mirror", status).value());
    }

    @Test
    void onlyTheOperatorCanTellWhichBackendAnswered() {
        Function<RestClient, ResponseEntity<String>> diagnostics = client ->
                client.get().uri("/manage/backend").retrieve().toEntity(String.class);
        String legacy = against("mainframe", diagnostics).getBody();
        String mirror = against("mirror", diagnostics).getBody();
        assertEquals("{\"backend\":\"mainframe\"}", legacy);
        assertNotEquals(legacy, mirror);
    }
}
