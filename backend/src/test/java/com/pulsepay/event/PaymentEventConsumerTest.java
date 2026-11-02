package com.pulsepay.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentEventConsumerTest {
    @Test
    void duplicate_event_stops_after_processed_event_claim() throws Exception {
        JdbcTemplate db = mock(JdbcTemplate.class);
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        when(db.update(startsWith("insert into processed_events"), eq(eventId))).thenReturn(0);
        PaymentEventConsumer consumer = new PaymentEventConsumer(db, new ObjectMapper());

        consumer.consume("{\"event_id\":\"" + eventId + "\",\"merchant_id\":\"" + merchantId + "\",\"event_type\":\"payment.succeeded\",\"data\":{}}");

        verify(db).update(startsWith("insert into processed_events"), eq(eventId));
        verifyNoMoreInteractions(db);
    }

    @Test
    void malformed_event_is_rejected_before_any_database_effect() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        PaymentEventConsumer consumer = new PaymentEventConsumer(db, new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> consumer.consume("{\"event_id\":\"not-a-uuid\"}"));
        verifyNoMoreInteractions(db);
    }
}
