package com.pulsepay.customer;

import com.pulsepay.common.ApiException;
import com.pulsepay.security.MerchantPrincipal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final JdbcTemplate db;
    public CustomerController(JdbcTemplate db) { this.db = db; }
    record Input(String email, String name) { }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@AuthenticationPrincipal MerchantPrincipal p, @RequestBody Input input) {
        UUID id = UUID.randomUUID();
        db.update("insert into customers(id,merchant_id,email,name)values(?,?,?,?)", id, p.merchantId(), input.email(), input.name());
        return customer(p, id);
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal MerchantPrincipal p) {
        return db.query("select id,email,name,created_at,updated_at from customers where merchant_id=? order by created_at desc", (rs, i) -> row(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toString(), rs.getTimestamp(5).toInstant().toString()), p.merchantId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable UUID id) { return customer(p, id); }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable UUID id, @RequestBody Input input) {
        int changed = db.update("update customers set email=coalesce(?,email),name=coalesce(?,name),updated_at=now() where id=? and merchant_id=?", input.email(), input.name(), id, p.merchantId());
        if (changed == 0) throw new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer was not found.");
        return customer(p, id);
    }

    @GetMapping("/{id}/payments")
    public List<Map<String, Object>> payments(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable UUID id) {
        customer(p, id);
        return db.query("select public_id,amount_minor,currency,status,description,created_at from payment_intents where merchant_id=? and customer_id=? order by created_at desc", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4), "description", Optional.ofNullable(rs.getString(5)).orElse(""), "created_at", rs.getTimestamp(6).toInstant().toString()), p.merchantId(), id);
    }

    @GetMapping("/{id}/refunds")
    public List<Map<String, Object>> refunds(@AuthenticationPrincipal MerchantPrincipal p, @PathVariable UUID id) {
        customer(p, id);
        return db.query("select r.public_id,r.amount_minor,r.currency,r.status,r.created_at from refunds r join payment_intents pi on pi.id=r.payment_intent_id where pi.merchant_id=? and pi.customer_id=? order by r.created_at desc", (rs, i) -> Map.<String, Object>of("id", rs.getString(1), "amount_minor", rs.getLong(2), "currency", rs.getString(3), "status", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString()), p.merchantId(), id);
    }

    private Map<String, Object> customer(MerchantPrincipal p, UUID id) {
        List<Map<String, Object>> rows = db.query("select id,email,name,created_at,updated_at from customers where id=? and merchant_id=?", (rs, i) -> row(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toString(), rs.getTimestamp(5).toInstant().toString()), id, p.merchantId());
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer was not found.");
        return rows.getFirst();
    }
    private Map<String, Object> row(UUID id, String email, String name, String created, String updated) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", id); result.put("email", Optional.ofNullable(email).orElse("")); result.put("name", Optional.ofNullable(name).orElse("")); result.put("created_at", created); result.put("updated_at", updated); return result; }
}
