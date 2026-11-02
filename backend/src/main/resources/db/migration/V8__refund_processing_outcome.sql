ALTER TABLE refunds
    ADD COLUMN test_outcome VARCHAR(20) NOT NULL DEFAULT 'success'
    CHECK (test_outcome IN ('success', 'failed'));

CREATE INDEX refunds_pending_processing_idx ON refunds(status, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');
