package com.pulsepay.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {
    private final IdempotencyService service = new IdempotencyService(null, new ObjectMapper());

    @Test
    void fingerprints_are_stable_when_object_key_order_changes() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("amount_minor", 500L);
        first.put("metadata", Map.of("b", "two", "a", "one"));
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("metadata", Map.of("a", "one", "b", "two"));
        second.put("amount_minor", 500L);
        assertEquals(service.fingerprint(first), service.fingerprint(second));
    }

    @Test
    void fingerprints_change_when_financial_request_changes() {
        assertNotEquals(service.fingerprint(Map.of("amount_minor", 500L)), service.fingerprint(Map.of("amount_minor", 501L)));
    }
}
