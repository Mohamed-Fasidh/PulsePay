# Testing

Run backend tests with `mvn verify` from `backend`, and frontend checks with `npm ci && npm run lint && npm run test && npm run build` from `frontend`.

The integration suite uses Testcontainers for PostgreSQL, Kafka, and Redis when Docker is available. It verifies container startup, deferred ledger balance/immutability constraints, operation-scoped idempotency uniqueness, and transactional outbox commit/rollback behavior alongside focused unit coverage. The next expansion should add full HTTP-level database-lock, tenant-isolation, consumer-deduplication, and webhook-retry scenarios.

Container tests skip when Docker is unavailable, which keeps developer unit-test feedback usable on restricted machines. The CI workflow runs Maven verification, lockfile-based frontend checks, Compose configuration validation, and image builds; it must fail rather than silently skip a failing check.

