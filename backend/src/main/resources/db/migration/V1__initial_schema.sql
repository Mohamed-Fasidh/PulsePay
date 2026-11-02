CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    default_currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (default_currency ~ '^[A-Z]{3}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MERCHANT')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE merchant_memberships (
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT, role VARCHAR(30) NOT NULL DEFAULT 'OWNER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY (merchant_id, user_id)
);
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    email VARCHAR(320), name VARCHAR(255), metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX customers_merchant_id_idx ON customers(merchant_id);
CREATE TABLE payment_intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT, customer_id UUID REFERENCES customers(id) ON DELETE RESTRICT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0), currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    description VARCHAR(1024), status VARCHAR(40) NOT NULL CHECK (status IN ('REQUIRES_PAYMENT_METHOD','REQUIRES_CONFIRMATION','PROCESSING','SUCCEEDED','FAILED','CANCELED')),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb, version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX payment_intents_merchant_created_idx ON payment_intents(merchant_id, created_at DESC);
CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(255) NOT NULL, request_fingerprint CHAR(64) NOT NULL,
    response_status INTEGER, response_body JSONB, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (merchant_id, idempotency_key)
);
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_type VARCHAR(64) NOT NULL, aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL, correlation_id UUID NOT NULL, payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(), published_at TIMESTAMPTZ, attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX outbox_events_unpublished_idx ON outbox_events(occurred_at) WHERE published_at IS NULL;
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID REFERENCES merchants(id) ON DELETE RESTRICT,
    actor_user_id UUID REFERENCES users(id) ON DELETE RESTRICT, action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL, resource_id UUID, correlation_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_logs_merchant_created_idx ON audit_logs(merchant_id, created_at DESC);

