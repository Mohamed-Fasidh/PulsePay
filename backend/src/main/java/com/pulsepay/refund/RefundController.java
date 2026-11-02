package com.pulsepay.refund;

import com.pulsepay.common.ApiException;
import com.pulsepay.idempotency.IdempotencyService;
import com.pulsepay.security.MerchantPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RefundController {
    private final JdbcTemplate db;
    private final IdempotencyService idempotency;
    public RefundController(JdbcTemplate db, IdempotencyService idempotency) { this.db = db; this.idempotency = idempotency; }
    record RefundRequest(String paymentIntentId, long amountMinor, String testOutcome) { }

    @PostMapping("/refunds") @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@AuthenticationPrincipal MerchantPrincipal p, @RequestHeader(value = "Idempotency-Key", required = false) String key, @RequestBody RefundRequest request) {
        if (request.amountMinor() <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REFUND_AMOUNT", "Refund amount must be greater than zero.");
        var replay = idempotency.replayOrClaim(p.merchantId(), "refund.create", key, request);
        if (replay.isPresent()) return replay.get();
        List<Object[]> payments = db.query("select pi.id,pi.currency,a.captured_minor from payment_intents pi join authorizations a on a.payment_intent_id=pi.id where pi.merchant_id=? and pi.public_id=? and pi.status='SUCCEEDED' for update", (rs, i) -> new Object[]{UUID.fromString(rs.getString(1)), rs.getString(2), rs.getLong(3)}, p.merchantId(), request.paymentIntentId());
        if (payments.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "REFUNDABLE_PAYMENT_NOT_FOUND", "A successful captured payment was not found.");
        Object[] payment = payments.getFirst();
        Long used = db.queryForObject("select coalesce(sum(amount_minor),0) from refunds where payment_intent_id=? and status in ('PENDING','PROCESSING','SUCCEEDED')", Long.class, payment[0]);
        if (used + request.amountMinor() > (Long) payment[2]) throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_REFUNDABLE_AMOUNT", "The requested refund exceeds the remaining refundable amount.");
        String outcome = request.testOutcome() == null ? "success" : request.testOutcome().trim().toLowerCase();
        if (!Set.of("success", "failed").contains(outcome)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REFUND_TEST_OUTCOME", "test_outcome must be success or failed.");
        UUID id = UUID.randomUUID(); String publicId = "re_" + id.toString().replace("-", "");
        db.update("insert into refunds(id,public_id,merchant_id,payment_intent_id,amount_minor,currency,status,idempotency_key,test_outcome) values(?,?,?,?,?,?,'PENDING',?,?)", id, publicId, p.merchantId(), payment[0], request.amountMinor(), payment[1], key, outcome);
        db.update("insert into outbox_events(merchant_id,aggregate_type,aggregate_id,event_type,correlation_id,payload) values(?,'refund',?,'refund.created',gen_random_uuid(),jsonb_build_object('refund_id',?))", p.merchantId(), id, id);
        Map<String, Object> out = Map.of("id", publicId, "amount_minor", request.amountMinor(), "currency", payment[1], "status", "PENDING");
        idempotency.complete(p.merchantId(), "refund.create", key, out);
        db.update("insert into audit_logs(merchant_id,actor_user_id,action,resource_type,resource_id)values(?,?,?,'refund',?)", p.merchantId(), p.userId(), "refund.created", id);
        return out;
    }

    @GetMapping("/refunds") public List<Map<String, Object>> list(@AuthenticationPrincipal MerchantPrincipal p) { return db.query("select public_id,amount_minor,currency,status,created_at from refunds where merchant_id=? order by created_at desc", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString()), p.merchantId()); }
    @GetMapping("/refunds/{id}") public Map<String, Object> one(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable String id) { List<Map<String, Object>> rows = db.query("select public_id,amount_minor,currency,status,created_at,updated_at from refunds where merchant_id=? and public_id=?", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString(), "updated_at", rs.getTimestamp(6).toInstant().toString()), p.merchantId(), id); if(rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund was not found."); return rows.getFirst(); }
    @GetMapping("/payment-intents/{id}/refunds") public List<Map<String, Object>> forPayment(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable String id) { return db.query("select r.public_id,r.amount_minor,r.currency,r.status from refunds r join payment_intents pi on pi.id=r.payment_intent_id where pi.merchant_id=? and pi.public_id=?", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4)), p.merchantId(), id); }
}
