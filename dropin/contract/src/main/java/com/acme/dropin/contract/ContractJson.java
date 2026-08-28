package com.acme.dropin.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The JSON projection of the contract, byte-for-byte as the incumbent facade
 * emits it (verified against a running instance on the H2 local profile):
 * dates as {@code "2025-01-01"}, timestamps as epoch milliseconds, money
 * unquoted with two decimals, properties in model declaration order, no
 * pretty printing.
 */
public final class ContractJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private ContractJson() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + value, e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize into " + type.getSimpleName(), e);
        }
    }
}
