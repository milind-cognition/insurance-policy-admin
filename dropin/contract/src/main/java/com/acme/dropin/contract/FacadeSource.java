package com.acme.dropin.contract;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the caller-visible shape of the incumbent REST facade out of
 * {@code java-facade/src/main/java}, so that the OpenAPI document in this
 * module is derived from the controller rather than written alongside it.
 *
 * <p>The facade is Java 8 / Spring Boot 1.5 and is deliberately not modified,
 * not recompiled and not linked against by this module; it is read as source.
 */
public final class FacadeSource {

    private static final String CONTROLLER = "com.acme.insurance.pas.controller.PolicyController";
    private static final String POLICY_MODEL = "com.acme.insurance.pas.model.Policy";
    private static final String COVERAGE_MODEL = "com.acme.insurance.pas.model.Coverage";
    private static final String REPOSITORY = "com.acme.insurance.pas.repository.PolicyRepository";

    private static final Pattern REQUEST_MAPPING = Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)");
    private static final Pattern GET_MAPPING = Pattern.compile(
            "@GetMapping\\(\"([^\"]*)\"\\)\\s*public\\s+ResponseEntity<([^>]+(?:>)?)>\\s+(\\w+)\\s*\\(",
            Pattern.DOTALL);
    private static final Pattern MODEL_FIELD = Pattern.compile(
            "^\\s*private\\s+([\\w.<>]+)\\s+(\\w+)\\s*;\\s*$", Pattern.MULTILINE);
    private static final Pattern ROW_MAPPING = Pattern.compile(
            "\\.set(\\w+)\\(\\s*rs\\.get(\\w+)\\(");
    private static final Pattern NOT_FOUND = Pattern.compile("HttpStatus\\.NOT_FOUND");

    /** One field of a response body, with the JSON type the facade actually emits. */
    public record ModelField(String name, String javaType, String jdbcAccessor) {
    }

    /** One caller-visible operation. */
    public record Operation(String method, String path, String operationId,
                            String responseType, boolean returnsNotFound) {
    }

    private final String controllerSource;
    private final String repositorySource;
    private final Path root;

    public FacadeSource() {
        this(RepoLayout.repoRoot());
    }

    public FacadeSource(Path repoRoot) {
        this.root = repoRoot;
        this.controllerSource = read(source(CONTROLLER));
        this.repositorySource = read(source(REPOSITORY));
    }

    private Path source(String fqn) {
        return root.resolve("java-facade/src/main/java").resolve(fqn.replace('.', '/') + ".java");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read facade source " + path, e);
        }
    }

    public String basePath() {
        Matcher m = REQUEST_MAPPING.matcher(controllerSource);
        if (!m.find()) {
            throw new IllegalStateException("No @RequestMapping on PolicyController");
        }
        return m.group(1);
    }

    public List<Operation> operations() {
        List<Operation> ops = new ArrayList<>();
        Matcher m = GET_MAPPING.matcher(controllerSource);
        while (m.find()) {
            String path = basePath() + m.group(1);
            String responseType = m.group(2).trim();
            String name = m.group(3);
            String body = methodBody(m.end());
            ops.add(new Operation("get", path, name, responseType, NOT_FOUND.matcher(body).find()));
        }
        if (ops.isEmpty()) {
            throw new IllegalStateException("No @GetMapping methods found on PolicyController");
        }
        return ops;
    }

    private String methodBody(int from) {
        int open = controllerSource.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < controllerSource.length(); i++) {
            char c = controllerSource.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return controllerSource.substring(open, i + 1);
            }
        }
        throw new IllegalStateException("Unbalanced braces in PolicyController");
    }

    public List<ModelField> policyFields() {
        return modelFields(POLICY_MODEL);
    }

    public List<ModelField> coverageFields() {
        return modelFields(COVERAGE_MODEL);
    }

    private List<ModelField> modelFields(String fqn) {
        Map<String, String> accessors = jdbcAccessors();
        List<ModelField> fields = new ArrayList<>();
        Matcher m = MODEL_FIELD.matcher(read(source(fqn)));
        while (m.find()) {
            String name = m.group(2);
            fields.add(new ModelField(name, m.group(1), accessors.get(name.toLowerCase())));
        }
        return fields;
    }

    /**
     * Setter name to JDBC getter used by the repository row mappers. This is
     * what decides whether a {@code java.util.Date} field is serialized as an
     * ISO date or as epoch milliseconds: the declared type does not say.
     */
    public Map<String, String> jdbcAccessors() {
        Map<String, String> accessors = new LinkedHashMap<>();
        Matcher m = ROW_MAPPING.matcher(repositorySource);
        while (m.find()) {
            accessors.put(m.group(1).toLowerCase(), m.group(2));
        }
        return accessors;
    }
}
