# Developer test events

Use this endpoint to test an already-configured webhook integration without creating a
real payment request or sending real payment information.

```http
POST /api/v1/developer/test-events
Authorization: Bearer <merchant-access-token>
Content-Type: application/json

{
  "eventType": "payment.succeeded",
  "paymentIntentId": "pi_optional_reference"
}
```

Supported event types are `payment.succeeded`, `payment.failed`, `payment.captured`,
`refund.created`, `refund.succeeded`, and `refund.failed`. The endpoint returns a
synthetic event ID and queues the event with `test_mode: true` via the transactional
outbox. It is tenant-scoped, audited, and does not create a payment, refund, capture,
or ledger posting.

For deterministic payment-engine scenarios instead, create a payment intent with
`metadata.test_outcome` set to `success`, `decline`, `timeout`, `temporary_failure`,
or `duplicate_event`. These are simulation controls only and must never be accepted as
payment credentials.

Refund creation accepts `testOutcome` of `success` (the default) or `failed` in test
mode. A refund first reserves capacity in `PENDING`, then the asynchronous refund
processor transitions it through `PROCESSING`. Only a `SUCCEEDED` refund posts its
balanced ledger journal; a `FAILED` refund releases the reservation and has no ledger
effect.
