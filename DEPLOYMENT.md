# Deployment

Use `docker compose up --build` for development. Supply secrets through the runtime environment, not version control. Production deployments require a managed PostgreSQL backup/retention policy, TLS termination, a Kafka cluster, external secret storage, and a real webhook worker with egress controls.

The Compose health checks include startup grace periods for PostgreSQL, Kafka, and the API. On a resource-constrained Docker Desktop installation, wait for those checks to settle before diagnosing a dependency failure; use `docker compose ps` and `docker compose logs <service>` to inspect the actual service state.

