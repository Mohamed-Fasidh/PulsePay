ALTER TABLE payment_intents
    ADD COLUMN processing_attempts INTEGER NOT NULL DEFAULT 0 CHECK (processing_attempts >= 0);

ALTER TABLE outbox_events
    ADD COLUMN dead_lettered_at TIMESTAMPTZ;

CREATE INDEX outbox_events_retry_idx ON outbox_events(occurred_at)
    WHERE published_at IS NULL AND dead_lettered_at IS NULL;
