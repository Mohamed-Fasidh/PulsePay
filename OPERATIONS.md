# PulsePay operations guide

## Runtime checks

The backend exposes Spring Boot Actuator health at `/actuator/health`, liveness at
`/actuator/health/liveness`, readiness at `/actuator/health/readiness`, and Prometheus
metrics at `/actuator/prometheus`. Health checks contain no payment, merchant, key, or
webhook-secret data.

The Compose stack provisions PostgreSQL, Redis, Kafka, the backend, the frontend,
OpenTelemetry Collector, Prometheus, and Grafana. PostgreSQL, Redis, Kafka, backend,
and frontend each have a health check. PostgreSQL and Grafana retain data in named
volumes. The backend applies Flyway migrations during startup.

## Metrics and logs

The `PulsePay Operations` Grafana dashboard includes request throughput and latency,
HTTP errors, JVM health, Kafka lag, payment/refund outcome rates, outbox backlog,
webhook backlog/dead-lettered deliveries, and webhook delivery latency. Financial and
delivery metric labels are deliberately low-cardinality and never contain merchant IDs,
payment IDs, webhook URLs, credentials, or payload values.

Console logs are structured with timestamp, log level, logger, and `request_id` MDC
field. Operators should use correlation IDs from the audit/outbox records when tracing a
payment workflow; secrets, authorization headers, API key material, and raw payment
method data must never be added to logs.

## Event recovery

Payment, capture, refund, and synthetic test events are written to `outbox_events` in
the same database transaction as the corresponding financial state change. The outbox
publisher claims pending records, publishes them to Kafka, and records failures for
retry. The consumer records processed event IDs before creating webhook delivery work,
so duplicate Kafka deliveries do not create duplicate webhook events.

Failed webhook attempts are persisted with HTTP status and attempt count. Retriable
failures use exponential backoff. Events exceeding the configured attempt limit move to
the dead-letter state and can be manually retried from the developer API/dashboard.
Any sustained backlog should be investigated by checking database health, Kafka
availability, and the delivery endpoint before retrying or replaying records.

## Financial recovery rules

Never repair a posted journal by editing a ledger entry. Use the ledger reversal API to
post the equal-and-opposite journal, then create a new correcting transaction if needed.
Do not directly alter payment, capture, or refund status in PostgreSQL. This bypasses
locking, idempotency, audit history, outbox publication, and accounting safeguards.

The application stores integer minor units. Currency validation and the API contract
define what a supplied minor unit means; no floating-point money values are accepted.
When reviewing an incident, reconcile capture and refund totals against ledger journals
by transaction reference and verify that every journal's debits equal credits.

## Test-mode event simulation

`POST /api/v1/developer/test-events` creates a merchant-scoped synthetic event through
the normal transactional-outbox path. It accepts only documented payment/refund event
types and includes a `test_mode` payload marker. It does not contact payment networks,
accept PAN/CVV data, or create a financial ledger movement. Use payment metadata
`test_outcome` for deterministic simulator outcomes such as `success`, `decline`,
`timeout`, `temporary_failure`, and `duplicate_event`.
