package com.pulsepay.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pulsepay.common.ApiException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Persists response replays and rejects accidental key reuse with another request body. */
@Service
public class IdempotencyService {
    private final JdbcTemplate db;
    private final ObjectMapper canonicalJson;
    private final ObjectMapper json;

    public IdempotencyService(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
        this.canonicalJson = json.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public Optional<Map<String, Object>> replayOrClaim(UUID merchantId, String operation, String key, Object request) {
        if (key == null || key.isBlank()) return Optional.empty();
        if (key.length() > 255) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be 255 characters or fewer.");
        String fingerprint = fingerprint(request);
        db.update("delete from idempotency_records where merchant_id=? and idempotency_key=? and operation=? and expires_at <= now()", merchantId, key, operation);
        var prior = db.query("select request_fingerprint,response_body from idempotency_records where merchant_id=? and idempotency_key=? and operation=?", (rs, i) -> new Object[]{rs.getString(1), rs.getString(2)}, merchantId, key, operation);
        if (!prior.isEmpty()) return replay(prior.getFirst(), fingerprint);

        int claimed = db.update("insert into idempotency_records(merchant_id,idempotency_key,operation,request_fingerprint,expires_at) values(?,?,?,?,now()+interval '24 hours') on conflict (merchant_id,idempotency_key,operation) do nothing", merchantId, key, operation, fingerprint);
        if (claimed == 1) return Optional.empty();
        prior = db.query("select request_fingerprint,response_body from idempotency_records where merchant_id=? and idempotency_key=? and operation=?", (rs, i) -> new Object[]{rs.getString(1), rs.getString(2)}, merchantId, key, operation);
        if (prior.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", "The idempotent request is still being processed.");
        return replay(prior.getFirst(), fingerprint);
    }

    public void complete(UUID merchantId, String operation, String key, Object response) {
        if (key == null || key.isBlank()) return;
        try {
            db.update("update idempotency_records set response_status=201,response_body=cast(? as jsonb) where merchant_id=? and idempotency_key=? and operation=?", json.writeValueAsString(response), merchantId, key, operation);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist idempotent response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> replay(Object[] record, String fingerprint) {
        if (!fingerprint.equals(record[0])) throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "This Idempotency-Key was already used with a different request.");
        if (record[1] == null) throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", "The idempotent request is still being processed.");
        try { return Optional.of(json.readValue((String) record[1], Map.class)); }
        catch (Exception e) { throw new IllegalStateException("Unable to read idempotent response", e); }
    }

    String fingerprint(Object request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonicalJson.writeValueAsBytes(request));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) { throw new IllegalStateException("Unable to fingerprint request", e); }
    }
}
