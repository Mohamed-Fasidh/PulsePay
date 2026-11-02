# API documentation

All endpoints use `/api/v1`. Protected endpoints require `Authorization: Bearer <access_token>`. Amounts are integer minor units and currencies are uppercase ISO-4217 codes.

## Authentication

`POST /auth/register` accepts `email`, `password` (minimum 12 characters), `legalName`, and `displayName`; it creates the merchant and returns access/refresh tokens. `POST /auth/login` and `POST /auth/refresh` rotate session credentials. `POST /auth/logout` accepts `{ "refreshToken": "..." }` and revokes that refresh session.

## Financial operations

- `POST /payment-intents`, `POST /payment-intents/{id}/capture`, and `POST /refunds` accept `Idempotency-Key`. A replay with the same request returns the original response; the same key with a different request returns `409 IDEMPOTENCY_KEY_REUSED`.
- Programmatic clients may send a generated test secret in `X-Api-Key`. JWTs remain required for developer, webhook, and audit administration.
- `PATCH /webhooks/{id}` can change a webhook URL, event types, or `enabled` state. `POST /webhooks/{id}/rotate-secret` returns a replacement signing secret once.
- `GET /customers/{id}/payments` and `GET /customers/{id}/refunds` return merchant-scoped financial history for a customer.
- `GET` and `PATCH /merchant/profile` expose the authenticated merchant’s own profile and default settlement currency.
- `GET /payment-intents` supports `status`, `currency`, `query`, `page` (1-based), and `size` (1–100) parameters. Search matches the public ID or description.
- `GET /payment-intents/{id}/details` returns the merchant-scoped payment record together with authorization, captures, refunds, associated journals, and audit timeline.
- `POST` and `GET /payment-methods` manage simulated provider references only. They never accept or store card numbers, CVVs, bank numbers, or real payment credentials.
- `POST /api-keys/{id}/rotate` revokes a current test key and returns a replacement secret exactly once.
- `POST /ledger/journals/{reference}/reverse` creates an immutable compensating journal; posted ledger records are never updated or deleted.

## Webhook verification

Deliveries include `PulsePay-Event-Id` and a `PulsePay-Signature` header in the form `t=<unix-seconds>,v1=<hex-hmac>`. Verify the HMAC-SHA256 of the exact UTF-8 string `<timestamp>.<raw-request-body>` using the endpoint secret, compare it in constant time, and reject stale timestamps. Webhook endpoints may be disabled or their secret rotated from the developer API; a rotated secret is shown only in that rotation response.

```text
expected = HMAC_SHA256_HEX(webhook_secret, timestamp + "." + raw_body)
accept only if constant_time_equal(expected, signature.v1)
```
- `POST /payment-intents/{id}/confirm` changes an eligible intent to processing. The local simulator settles it asynchronously; descriptions containing `decline` deterministically fail.
- `POST /payment-intents/{id}/cancel` cancels an unprocessed intent.
- `POST /payment-intents/{id}/capture` captures all or part of an authorized payment.
- `POST /payment-intents/{id}/cancel-authorization` cancels an uncaptured authorization.
- `POST /refunds` accepts `paymentIntentId` and `amountMinor` and uses a row lock to prevent over-refunding; `GET /refunds/{id}` reads a merchant-scoped refund.
- `GET /ledger` returns immutable debit/credit lines.
- `POST /customers`, `GET /customers`, `GET /customers/{id}`, and `PATCH /customers/{id}` manage merchant-scoped simulated customers.
- `POST /api-keys` returns a test secret exactly once; `POST /api-keys/{id}/revoke` permanently revokes an active key.

Error envelopes use `{ "error": { "code", "message", "request_id" } }`. Merchant ownership is enforced on every resource query.

## Webhook verification

Deliveries use the `PulsePay-Signature` header in the form `t=<unix-seconds>,v1=<hex-hmac>`, plus `PulsePay-Event-Id`. Verify the HMAC-SHA256 signature over `timestamp + "." + rawRequestBody` with the secret shown once when the endpoint is created. Reject stale timestamps to protect against replay. Deliveries time out after ten seconds and retry with exponential backoff before entering the dead-letter state.

Use `GET /webhooks/{id}/deliveries` to inspect delivery attempts and `POST /webhooks/deliveries/{deliveryId}/retry` to place a retryable dead-letter/temporary failure back in the delivery queue.

