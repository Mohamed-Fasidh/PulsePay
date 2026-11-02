package com.pulsepay.dashboard;

import com.pulsepay.common.ApiException;
import com.pulsepay.security.MerchantPrincipal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final JdbcTemplate db;
    public DashboardController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/overview")
    public Map<String, Object> overview(@AuthenticationPrincipal MerchantPrincipal principal, @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange range = range(from, to);
        Timestamp fromTimestamp = Timestamp.from(range.from());
        Timestamp toTimestamp = Timestamp.from(range.to());
        return db.queryForObject("select coalesce(sum(amount_minor) filter(where status='SUCCEEDED'),0) gross,coalesce(sum(amount_minor) filter(where status='FAILED'),0) failed,coalesce(sum(amount_minor) filter(where status='PROCESSING'),0) pending,count(*) filter(where status='SUCCEEDED') ok,count(*) total from payment_intents where merchant_id=? and created_at>=? and created_at<?", (rs, i) -> {
            long refunds = db.queryForObject("select coalesce(sum(amount_minor),0) from refunds where merchant_id=? and status='SUCCEEDED' and created_at>=? and created_at<?", Long.class, principal.merchantId(), fromTimestamp, toTimestamp);
            long gross = rs.getLong("gross");
            return Map.of("gross_volume_minor", gross, "failed_volume_minor", rs.getLong("failed"), "pending_volume_minor", rs.getLong("pending"), "refund_volume_minor", refunds, "net_volume_minor", gross - refunds, "successful_payments", rs.getLong("ok"), "success_rate", rs.getLong("total") == 0 ? 0 : rs.getLong("ok") * 100.0 / rs.getLong("total"));
        }, principal.merchantId(), fromTimestamp, toTimestamp);
    }

    @GetMapping("/trends")
    public List<Map<String, Object>> trends(@AuthenticationPrincipal MerchantPrincipal principal, @RequestParam(defaultValue = "30") int days, @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange range = from == null && to == null ? new DateRange(Instant.now().minusSeconds(Math.max(1, Math.min(days, 90)) * 86400L), Instant.now().plusSeconds(1)) : range(from, to);
        Timestamp fromTimestamp = Timestamp.from(range.from());
        Timestamp toTimestamp = Timestamp.from(range.to());
        return db.query("select to_char(created_at at time zone 'UTC','YYYY-MM-DD') day,coalesce(sum(amount_minor) filter(where status='SUCCEEDED'),0) volume_minor,count(*) filter(where status='SUCCEEDED') successful,count(*) filter(where status='FAILED') failed from payment_intents where merchant_id=? and created_at>=? and created_at<? group by 1 order by 1", (rs, i) -> Map.<String, Object>of("date", rs.getString(1), "volume_minor", rs.getLong(2), "successful", rs.getLong(3), "failed", rs.getLong(4)), principal.merchantId(), fromTimestamp, toTimestamp);
    }

    @GetMapping("/status-distribution")
    public List<Map<String, Object>> distribution(@AuthenticationPrincipal MerchantPrincipal principal, @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange range = range(from, to);
        Timestamp fromTimestamp = Timestamp.from(range.from());
        Timestamp toTimestamp = Timestamp.from(range.to());
        return db.query("select status,count(*) count from payment_intents where merchant_id=? and created_at>=? and created_at<? group by status order by count desc", (rs, i) -> Map.<String, Object>of("status", rs.getString(1), "count", rs.getLong(2)), principal.merchantId(), fromTimestamp, toTimestamp);
    }

    @GetMapping("/recent-payments")
    public List<Map<String, Object>> recentPayments(@AuthenticationPrincipal MerchantPrincipal principal) {
        return db.query("select public_id,amount_minor,currency,status,description,created_at from payment_intents where merchant_id=? order by created_at desc limit 8", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4), "description", Optional.ofNullable(rs.getString(5)).orElse(""), "created_at", rs.getTimestamp(6).toInstant().toString()), principal.merchantId());
    }

    @GetMapping("/recent-webhook-activity")
    public List<Map<String, Object>> recentWebhookActivity(@AuthenticationPrincipal MerchantPrincipal principal) {
        return db.query("select d.event_type,d.status,d.attempts,d.last_http_status,d.created_at from webhook_deliveries d join webhook_endpoints w on w.id=d.webhook_endpoint_id where w.merchant_id=? order by d.created_at desc limit 8", (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("event_type", rs.getString(1)); row.put("status", rs.getString(2)); row.put("attempts", rs.getInt(3)); row.put("http_status", rs.getObject(4)); row.put("created_at", rs.getTimestamp(5).toInstant().toString());
            return row;
        }, principal.merchantId());
    }

    private DateRange range(String from, String to) {
        try {
            LocalDate start = from == null ? LocalDate.now().minusDays(30) : LocalDate.parse(from);
            LocalDate end = to == null ? LocalDate.now().plusDays(1) : LocalDate.parse(to).plusDays(1);
            if (!end.isAfter(start) || start.plusDays(366).isBefore(end)) throw new IllegalArgumentException();
            return new DateRange(start.atStartOfDay(ZoneOffset.UTC).toInstant(), end.atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (Exception exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "Use ISO dates with a range no longer than 366 days."); }
    }
    private record DateRange(Instant from, Instant to) { }
}
