# Payment lifecycle

`REQUIRES_CONFIRMATION → PROCESSING → SUCCEEDED | FAILED`; cancellation is available only before processing. Successful simulated processing creates an `AUTHORIZED` authorization. Capture is a separate operation and can be partial, but cannot exceed the remaining authorization. An uncaptured authorization can be cancelled. Each successful capture posts a balanced journal. Refunds transition `PENDING → PROCESSING → SUCCEEDED | FAILED`; pending and processing refunds reserve refundable capacity, while only successful refunds post a reversal-style journal.

The processor is deliberately deterministic: including `decline` in a test payment description produces a failure. This avoids randomness in automated tests.

# Payment and event lifecycle

1. A merchant creates a `REQUIRES_CONFIRMATION` payment intent using an `Idempotency-Key`.
2. Confirmation atomically moves it to `PROCESSING` and writes a merchant-scoped transactional-outbox event.
3. The local test-mode provider consumes processing work asynchronously. `metadata.test_outcome` can select `success`, `decline`, `timeout`, `temporary_failure`, or `duplicate_event`; randomized outcomes are disabled by default.
4. A successful simulation creates an authorization. Capture creates the balanced payment journal. Refund processing is asynchronous; a successful refund creates its own balanced refund journal, while a failed refund creates no ledger posting.
5. The outbox publisher emits an envelope containing `event_id`, `merchant_id`, `event_type`, and payload. It retries delivery and sends exhausted publish attempts to `payment.events.DLT`.
6. The webhook consumer records an event once in `processed_events`, persists it in `webhook_events`, and selects only enabled endpoints owned by that event’s merchant. Transient consumer failures retry three times with a one-second backoff before the original Kafka record is sent to `payment.events.DLT`.
7. Webhook delivery signs the raw JSON with HMAC-SHA256 and retries failures with exponential backoff before dead-lettering.

The database transaction is the source of truth. Kafka and webhook delivery are asynchronous projections, and duplicate event delivery is safe because consumer event IDs and financial journal references are unique.
