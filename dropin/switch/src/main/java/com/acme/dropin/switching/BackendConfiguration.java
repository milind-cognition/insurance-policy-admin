package com.acme.dropin.switching;

import com.acme.dropin.contract.ContractJson;
import com.acme.dropin.contract.PasBackend;
import com.acme.dropin.legacy.LegacyPolicyAdministration;
import com.acme.dropin.mirror.MirrorDatabase;
import com.acme.dropin.mirror.MirrorPolicyAdministration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The one place the choice is made. {@code pas.backend} is read here and
 * nowhere else; every other class in this module talks to {@link PasBackend}.
 *
 * <p>That is the shape a real cutover takes: the seam is a single binding, so
 * routing a slice of traffic, or rolling back, is a property change rather than
 * a release.
 */
@Configuration
public class BackendConfiguration {

    public static final String MAINFRAME = "mainframe";
    public static final String MIRROR = "mirror";

    /**
     * The contract's mapper, not a fresh one: date, money and property-order
     * formatting are part of what a caller depends on, so the service writes
     * JSON through the same configuration the contract tests assert against.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return ContractJson.mapper();
    }

    @Bean
    public Clock demoClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasBackend pasBackend(@Value("${pas.backend:mainframe}") String backend, Clock clock) {
        return switch (backend) {
            case MAINFRAME -> LegacyPolicyAdministration.loadedWithDemoData(clock);
            case MIRROR -> new MirrorPolicyAdministration(
                    MirrorDatabase.of(mirrorUrl()).loadDemoData(), clock);
            default -> throw new IllegalArgumentException(
                    "pas.backend must be '" + MAINFRAME + "' or '" + MIRROR + "', not '" + backend + "'");
        };
    }

    /**
     * A named in-memory database per JVM. It is file-addressable too, which is
     * how the unmodified Spring Boot 1.5 facade is pointed at mirror data with
     * only a datasource property - see dropin/scripts/facade-against-mirror.sh.
     */
    private static String mirrorUrl() {
        return System.getProperty("pas.mirror.url", MirrorDatabase.IN_MEMORY_URL);
    }
}
