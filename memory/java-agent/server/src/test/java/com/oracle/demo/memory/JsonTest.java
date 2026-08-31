package com.oracle.demo.memory;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {
    @Test
    void roundTripsDemoActionPayload() throws Exception {
        Map<String, Object> body = Json.object(new ByteArrayInputStream(
                "{\"guestId\":\"AVA\",\"query\":\"rain-safe evening\"}"
                        .getBytes(StandardCharsets.UTF_8)));
        assertEquals("AVA", body.get("guestId"));
        assertTrue(Json.value(body).contains("rain-safe evening"));
    }
}
