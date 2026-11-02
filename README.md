# PulsePay

PulsePay is a production-style, simulated payment-processing and double-entry-ledger platform. It deliberately does not accept card numbers, CVVs, or connect to a real payment processor.

## Current implementation

The MVP includes merchant registration/login with JWT access and rotated refresh tokens, payment intent creation/confirmation/cancellation, deterministic local settlement, captured authorizations, locking-protected refunds, double-entry journals, operation-scoped idempotency for money-moving writes, API key creation and `X-Api-Key` authentication, webhook endpoint registration, dashboard aggregations, audit records, Docker development infrastructure, and CI.

## Local development

1. Copy `.env.example` to `.env` and replace the development JWT secret.
2. Run `docker compose up --build`.
3. Visit the web UI at `http://localhost:5173`, the API health endpoint at `http://localhost:8080/actuator/health`, and Prometheus at `http://localhost:9090`.

Grafana is available at `http://localhost:3000` and includes the provisioned PulsePay service-health dashboard. The OpenTelemetry collector accepts OTLP HTTP traces locally on port `4318`.

Interactive API documentation is available at `http://localhost:8080/swagger-ui/index.html`.

The frontend proxies `/api/*` to the backend within Docker, so browser credentials and dashboard calls stay on a single local origin.

## Design principles

- PostgreSQL is authoritative; Kafka carries durable asynchronous notifications through a transactional outbox.
- API amounts use integer minor units. The current test-mode API validates ISO-4217 uppercase currency codes; production currency-exponent policy must be selected before adding non-two-decimal settlement currencies.
- Financial records use restrictive foreign keys and will never be cascade-deleted.
- Idempotency is merchant-scoped and ties a key to a request fingerprint and stored response.

## Documentation

- [Architecture](ARCHITECTURE.md): components, transaction boundaries, outbox, and event flow.
- [API guide](API_DOCUMENTATION.md): authentication, idempotency, request/response examples, and webhook verification.
- [Database design](DATABASE_DESIGN.md): relational model, ledger constraints, and data relationships.
- [Payment lifecycle](PAYMENT_LIFECYCLE.md): state machines, simulator outcomes, Kafka recovery, capture, and refund processing.
- [Security](SECURITY.md): secret handling, tenant authorization, webhook safety, and operational hardening.
- [Testing](TESTING.md): unit, container, frontend, CI, and financial-invariant coverage.
- [Deployment](DEPLOYMENT.md): local and production deployment considerations.
- [Operations](OPERATIONS.md): health checks, metrics, Grafana, event recovery, and financial incident handling.
- [Retention](RETENTION.md): archival and deletion safeguards for financial, audit, and delivery records.
- [Developer test events](docs/developer-test-events.md): safe webhook and refund simulation controls.

## Test-mode boundaries

The local settlement worker is intentionally a simulated provider. It supports deterministic outcomes with `metadata.test_outcome` (`success`, `decline`, `timeout`, `temporary_failure`, or `duplicate_event`) and keeps randomized outcomes disabled by default. The system never collects real card numbers, CVVs, or live payment credentials.

Refunds support deterministic test outcomes of `success` or `failed`. They remain reserved while pending/processing, and only successful refunds create a ledger journal.

Before an internet-facing deployment, configure production secret storage, an externally managed Kafka/PostgreSQL/Redis topology, TLS termination, and a formal currency-exponent policy.

