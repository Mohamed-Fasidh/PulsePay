package com.pulsepay.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FinancialMetricsTest {
    @Test
    void publishes_aggregate_outcome_counters_without_sensitive_labels() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FinancialMetrics metrics = new FinancialMetrics(mock(JdbcTemplate.class), registry);

        metrics.paymentOutcome("succeeded");
        metrics.paymentOutcome("succeeded");
        metrics.refundOutcome("failed");
        metrics.webhookAttempt("success", 42);

        assertEquals(2D, registry.get("pulsepay.payment.outcomes").tag("outcome", "succeeded").counter().count());
        assertEquals(1D, registry.get("pulsepay.refund.outcomes").tag("outcome", "failed").counter().count());
        assertEquals(1D, registry.get("pulsepay.webhook.attempts").tag("outcome", "success").counter().count());
        assertEquals(1L, registry.get("pulsepay.webhook.delivery").timer().count());
    }
}
