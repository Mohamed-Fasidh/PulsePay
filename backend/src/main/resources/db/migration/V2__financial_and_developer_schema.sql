CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    token_hash CHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE authorizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), payment_intent_id UUID NOT NULL UNIQUE REFERENCES payment_intents(id) ON DELETE RESTRICT,
    authorized_minor BIGINT NOT NULL CHECK (authorized_minor > 0), captured_minor BIGINT NOT NULL DEFAULT 0 CHECK (captured_minor >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AUTHORIZED','PARTIALLY_CAPTURED','CAPTURED','CANCELED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CHECK (captured_minor <= authorized_minor)
);
CREATE TABLE captures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), authorization_id UUID NOT NULL REFERENCES authorizations(id) ON DELETE RESTRICT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0), idempotency_key VARCHAR(255), status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCEEDED','FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE (authorization_id, idempotency_key)
);
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT, payment_intent_id UUID NOT NULL REFERENCES payment_intents(id) ON DELETE RESTRICT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0), currency CHAR(3) NOT NULL, status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED','CANCELED')),
    idempotency_key VARCHAR(255), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (merchant_id, idempotency_key)
);
CREATE INDEX refunds_payment_intent_idx ON refunds(payment_intent_id);
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID REFERENCES merchants(id) ON DELETE RESTRICT,
    code VARCHAR(64) NOT NULL, name VARCHAR(255) NOT NULL, account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','REVENUE','EXPENSE')),
    currency CHAR(3) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE (merchant_id, code, currency)
);
CREATE TABLE ledger_journals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), transaction_reference VARCHAR(128) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT, event_type VARCHAR(64) NOT NULL,
    currency CHAR(3) NOT NULL, posted_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), journal_id UUID NOT NULL REFERENCES ledger_journals(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id) ON DELETE RESTRICT,
    debit_minor BIGINT NOT NULL DEFAULT 0 CHECK (debit_minor >= 0), credit_minor BIGINT NOT NULL DEFAULT 0 CHECK (credit_minor >= 0),
    description VARCHAR(512) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CHECK ((debit_minor = 0) <> (credit_minor = 0))
);
CREATE INDEX ledger_entries_journal_idx ON ledger_entries(journal_id);
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    public_id VARCHAR(64) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, secret_hash CHAR(64) NOT NULL, test_mode BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMPTZ, last_used_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    url VARCHAR(2048) NOT NULL, secret_hash CHAR(64) NOT NULL, event_types JSONB NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE RESTRICT,
    event_id UUID NOT NULL, event_type VARCHAR(128) NOT NULL, payload JSONB NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0, last_http_status INTEGER, next_attempt_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, processed_at TIMESTAMPTZ NOT NULL DEFAULT now());

