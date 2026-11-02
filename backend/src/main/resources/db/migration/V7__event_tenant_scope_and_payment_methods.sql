ALTER TABLE outbox_events
    ADD COLUMN merchant_id UUID REFERENCES merchants(id) ON DELETE RESTRICT;
CREATE INDEX outbox_events_merchant_idx ON outbox_events(merchant_id, occurred_at DESC);

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    customer_id UUID REFERENCES customers(id) ON DELETE RESTRICT,
    provider_reference VARCHAR(255) NOT NULL,
    method_type VARCHAR(40) NOT NULL DEFAULT 'SIMULATED',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (merchant_id, provider_reference)
);
CREATE INDEX payment_methods_customer_idx ON payment_methods(merchant_id, customer_id);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX webhook_events_merchant_idx ON webhook_events(merchant_id, occurred_at DESC);
