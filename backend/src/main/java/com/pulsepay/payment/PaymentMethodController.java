package com.pulsepay.payment;

import com.pulsepay.common.ApiException;
import com.pulsepay.security.MerchantPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Test-mode references only; this service never accepts card or bank credentials. */
@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {
    private final JdbcTemplate db;
    public PaymentMethodController(JdbcTemplate db) { this.db = db; }
    record Create(UUID customerId, String providerReference, String methodType) { }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@AuthenticationPrincipal MerchantPrincipal principal, @RequestBody Create request) {
        if (request.providerReference() == null || request.providerReference().isBlank() || request.providerReference().length() > 255) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD_REFERENCE", "A safe simulated provider reference is required.");
        if (request.customerId() != null && !exists(principal.merchantId(), request.customerId())) throw new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer was not found.");
        UUID id = UUID.randomUUID();
        try { db.update("insert into payment_methods(id,merchant_id,customer_id,provider_reference,method_type)values(?,?,?,?,?)", id, principal.merchantId(), request.customerId(), request.providerReference(), request.methodType() == null ? "SIMULATED" : request.methodType()); }
        catch (Exception exception) { throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_METHOD_REFERENCE_IN_USE", "This simulated provider reference already exists."); }
        return one(principal, id);
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal MerchantPrincipal principal, @RequestParam(required = false) UUID customerId) {
        return db.query("select id,customer_id,provider_reference,method_type,created_at from payment_methods where merchant_id=? and (? is null or customer_id=?) order by created_at desc", (rs, i) -> Map.<String, Object>of("id", rs.getObject(1).toString(), "customer_id", rs.getObject(2) == null ? "" : rs.getObject(2).toString(), "provider_reference", rs.getString(3), "method_type", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString()), principal.merchantId(), customerId, customerId);
    }

    private Map<String, Object> one(MerchantPrincipal principal, UUID id) {
        List<Map<String, Object>> rows = db.query("select id,customer_id,provider_reference,method_type,created_at from payment_methods where merchant_id=? and id=?", (rs, i) -> Map.<String, Object>of("id", rs.getObject(1).toString(), "customer_id", rs.getObject(2) == null ? "" : rs.getObject(2).toString(), "provider_reference", rs.getString(3), "method_type", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString()), principal.merchantId(), id);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_METHOD_NOT_FOUND", "Payment method was not found.");
        return rows.getFirst();
    }
    private boolean exists(UUID merchantId, UUID customerId) { Integer count = db.queryForObject("select count(*) from customers where merchant_id=? and id=?", Integer.class, merchantId, customerId); return count != null && count > 0; }
}
