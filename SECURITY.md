# Security baseline

No production credential is present in source control. `.env.example` contains placeholders only. Authentication uses BCrypt password hashes, signed JWT access tokens, rotating refresh tokens, and merchant-scoped database queries. Sensitive endpoints receive an in-process rate limit.

Webhook signing secrets are displayed once, stored encrypted for delivery, and never returned by list APIs. Webhook destinations must be public HTTPS hosts; loopback, local, link-local, site-local, multicast, and reserved network destinations are rejected during registration and before delivery. API keys are stored as SHA-256 hashes, accepted only through the `X-Api-Key` header, and are rejected after revocation. API keys, card data, CVVs, webhook secrets, and auth tokens must never be logged. Production deployment must replace development environment fallback keys with managed secrets and terminate TLS before the application.

## Data retention

Financial journals, refunds, payment intents, idempotency records, and audit logs are restrictive-FK records and are never cascade-deleted by the application. Operational payloads and event logs should be archived according to the merchant contract; idempotency records currently retain responses for 24 hours.

