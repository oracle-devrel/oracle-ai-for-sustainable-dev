package com.oracle.demo.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
    }

    static String value(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode JSON", exception);
        }
    }

    static Map<String, Object> object(InputStream input) throws IOException {
        if (input == null) return new LinkedHashMap<>();
        byte[] body = input.readAllBytes();
        if (body.length == 0) return new LinkedHashMap<>();
        return MAPPER.readValue(body, new TypeReference<>() {
        });
    }
}
