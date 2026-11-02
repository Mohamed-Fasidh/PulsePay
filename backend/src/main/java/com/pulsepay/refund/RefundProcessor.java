package com.pulsepay.refund;

import com.pulsepay.config.FinancialMetrics;
import com.pulsepay.ledger.LedgerService;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Simulates asynchronous refund-provider completion while preserving refundable-balance reservations. */
@Component
public class RefundProcessor {
    private final JdbcTemplate db;
    private final LedgerService ledger;
    private final FinancialMetrics metrics;

    public RefundProcessor(JdbcTemplate db, LedgerService ledger, FinancialMetrics metrics) {
        this.db = db;
        this.ledger = ledger;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelay = 1_500)
    @Transactional
    public void process() {
        List<Refund> pending = db.query(
            "select id,merchant_id,amount_minor,currency,test_outcome from refunds where status='PENDING' order by created_at limit 20 for update skip locked",
            (rs, index) -> new Refund(
                rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getLong(3), rs.getString(4), rs.getString(5)
            )
        );
        for (Refund refund : pending) {
            db.update("update refunds set status='PROCESSING',updated_at=now() where id=?", refund.id());
            if ("failed".equals(refund.outcome())) {
                db.update("update refunds set status='FAILED',updated_at=now() where id=?", refund.id());
                metrics.refundOutcome("failed");
                event(refund, "refund.failed");
                continue;
            }
            ledger.postRefund(refund.merchantId(), refund.id(), refund.amountMinor(), refund.currency());
            db.update("update refunds set status='SUCCEEDED',updated_at=now() where id=?", refund.id());
            metrics.refundOutcome("succeeded");
            event(refund, "refund.succeeded");
        }
    }

    private void event(Refund refund, String type) {
        db.update(
            "insert into outbox_events(merchant_id,aggregate_type,aggregate_id,event_type,correlation_id,payload) values(?,'refund',?,?,gen_random_uuid(),jsonb_build_object('refund_id',?))",
            refund.merchantId(), refund.id(), type, refund.id()
        );
    }

    private record Refund(UUID id, UUID merchantId, long amountMinor, String currency, String outcome) { }
}
