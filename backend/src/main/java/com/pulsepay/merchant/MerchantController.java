package com.pulsepay.merchant;

import com.pulsepay.common.ApiException;
import com.pulsepay.security.MerchantPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {
    private final JdbcTemplate db;
    public MerchantController(JdbcTemplate db) { this.db = db; }
    record ProfileInput(String legalName, String displayName, String defaultCurrency) { }

    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal MerchantPrincipal principal) {
        return find(principal);
    }

    @PatchMapping("/profile")
    public Map<String, Object> update(@AuthenticationPrincipal MerchantPrincipal principal, @RequestBody ProfileInput input) {
        if (input.defaultCurrency() != null && !input.defaultCurrency().matches("[A-Z]{3}")) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY", "default_currency must be an ISO-4217 uppercase code.");
        if (input.legalName() != null && input.legalName().isBlank() || input.displayName() != null && input.displayName().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MERCHANT_PROFILE", "Merchant names cannot be blank.");
        db.update("update merchants set legal_name=coalesce(?,legal_name),display_name=coalesce(?,display_name),default_currency=coalesce(?,default_currency),updated_at=now() where id=?", input.legalName(), input.displayName(), input.defaultCurrency(), principal.merchantId());
        db.update("insert into audit_logs(merchant_id,actor_user_id,action,resource_type,resource_id)values(?,?,?,'merchant',?)", principal.merchantId(), principal.userId(), "merchant.profile_updated", principal.merchantId());
        return find(principal);
    }

    private Map<String, Object> find(MerchantPrincipal principal) {
        List<Map<String, Object>> rows = db.query("select id,legal_name,display_name,default_currency,created_at,updated_at from merchants where id=?", (rs, i) -> Map.<String, Object>of("id", rs.getObject(1).toString(), "legal_name", rs.getString(2), "display_name", rs.getString(3), "default_currency", rs.getString(4), "created_at", rs.getTimestamp(5).toInstant().toString(), "updated_at", rs.getTimestamp(6).toInstant().toString()), principal.merchantId());
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", "Merchant was not found.");
        return rows.getFirst();
    }
}
