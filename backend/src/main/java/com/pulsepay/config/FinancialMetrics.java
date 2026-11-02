package com.pulsepay.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aggregate operational metrics only. Tags deliberately exclude merchant IDs,
 * payment IDs, credentials, URLs, and other high-cardinality or sensitive data.
 */
@Component
public class FinancialMetrics {
    private final JdbcTemplate db;
    private final MeterRegistry meters;

    public FinancialMetrics(JdbcTemplate db, MeterRegistry meters) {
        this.db = db;
        this.meters = meters;
        Gauge.builder("pulsepay.outbox.pending", this, FinancialMetrics::pendingOutbox).register(meters);
        Gauge.builder("pulsepay.webhook.pending", this, FinancialMetrics::pendingWebhooks).register(meters);
        Gauge.builder("pulsepay.webhook.dead_lettered", this, FinancialMetrics::deadLetteredWebhooks).register(meters);
        Gauge.builder("pulsepay.webhook.delivery.latency", this, FinancialMetrics::averageWebhookDeliverySeconds).register(meters);
    }

    public void paymentOutcome(String outcome) {
        meters.counter("pulsepay.payment.outcomes", "outcome", outcome).increment();
    }

    public void refundOutcome(String outcome) {
        meters.counter("pulsepay.refund.outcomes", "outcome", outcome).increment();
    }

    public void webhookAttempt(String outcome, long elapsedMillis) {
        meters.counter("pulsepay.webhook.attempts", "outcome", outcome).increment();
        Timer.builder("pulsepay.webhook.delivery").publishPercentileHistogram().register(meters)
            .record(Math.max(0, elapsedMillis), TimeUnit.MILLISECONDS);
    }

    private double pendingOutbox() {
        return count("select count(*) from outbox_events where published_at is null and dead_lettered_at is null");
    }

    private double pendingWebhooks() {
        return count("select count(*) from webhook_deliveries where status in ('PENDING','RETRYING')");
    }

    private double deadLetteredWebhooks() {
        return count("select count(*) from webhook_deliveries where status='DEAD_LETTER'");
    }

    private double averageWebhookDeliverySeconds() {
        try {
            Double value = db.queryForObject(
                "select coalesce(avg(extract(epoch from (updated_at-created_at))),0) from webhook_deliveries "
                    + "where last_http_status between 200 and 299",
                Double.class
            );
            return value == null ? 0 : value;
        } catch (RuntimeException unavailableDuringStartup) {
            return 0;
        }
    }

    private double count(String sql) {
        try {
            Long value = db.queryForObject(sql, Long.class);
            return value == null ? 0 : value;
        } catch (RuntimeException unavailableDuringStartup) {
            return 0;
        }
    }
}
