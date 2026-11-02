ALTER TABLE idempotency_records
    ADD COLUMN operation VARCHAR(128) NOT NULL DEFAULT 'legacy';

ALTER TABLE idempotency_records
    DROP CONSTRAINT idempotency_records_merchant_id_idempotency_key_key;

ALTER TABLE idempotency_records
    ADD CONSTRAINT idempotency_records_merchant_key_operation_key
    UNIQUE (merchant_id, idempotency_key, operation);

CREATE INDEX idempotency_records_expiry_idx ON idempotency_records(expires_at);
