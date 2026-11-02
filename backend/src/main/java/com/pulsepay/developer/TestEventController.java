package com.pulsepay.developer;

import com.pulsepay.common.ApiException;
import com.pulsepay.security.MerchantPrincipal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes a synthetic webhook event through the normal transactional-outbox path in test mode. */
@RestController
@RequestMapping("/api/v1/developer/test-events")
public class TestEventController {
    private static final Set<String> ALLOWED_EVENTS = Set.of(
        "payment.succeeded", "payment.failed", "payment.captured",
        "refund.created", "refund.succeeded", "refund.failed"
    );

    private final JdbcTemplate db;

    public TestEventController(JdbcTemplate db) {
        this.db = db;
    }

    record TestEventRequest(String eventType, String paymentIntentId) { }

    @PostMapping
    @Transactional
    public Map<String, Object> send(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @RequestBody TestEventRequest request
    ) {
        String eventType = request.eventType() == null ? "" : request.eventType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EVENTS.contains(eventType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEST_EVENT", "Choose a supported payment or refund test event.");
        }

        UUID eventId = UUID.randomUUID();
        db.update(
            "insert into outbox_events(merchant_id,aggregate_type,aggregate_id,event_type,correlation_id,payload) "
                + "values(?,'test_event',?,?,?,jsonb_build_object('event_id',?,'test_mode',true,'payment_intent_id',?))",
            principal.merchantId(), eventId, eventType, eventId, eventId, request.paymentIntentId()
        );
        db.update(
            "insert into audit_logs(merchant_id,actor_user_id,action,resource_type,resource_id,metadata) "
                + "values(?,?,?,'test_event',?,jsonb_build_object('event_type',?))",
            principal.merchantId(), principal.userId(), "developer.test_event.sent", eventId, eventType
        );
        return Map.of("event_id", eventId.toString(), "event_type", eventType, "queued", true, "test_mode", true);
    }
}
