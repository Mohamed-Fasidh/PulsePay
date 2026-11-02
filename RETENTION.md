# Data retention and archival policy

PulsePay treats payments, captures, refunds, ledger journals/entries, idempotency
records, outbox events, webhook delivery records, and audit logs as operational and
financial evidence. Application code does not hard-delete these records and foreign-key
relationships use restrictive deletion behavior for financial data.

## Default retention

| Record class | Online retention | Archival rule |
| --- | --- | --- |
| Payment, capture, refund, authorization, ledger journal and ledger entry | Indefinite by default | Archive only through a controlled, reconciled export; retain immutable references and checksums. |
| Audit log | Seven years or longer when policy/regulation requires | Append-only export to encrypted, access-controlled storage. |
| Webhook event and delivery history | Ninety days online | Export aggregate diagnostics and retain dead-letter evidence before pruning. |
| Outbox and processed-event deduplication records | Thirty days after terminal processing, subject to replay requirements | Archive/reconcile before pruning; never remove a pending or failed record. |
| Idempotency records | At least twenty-four hours online | Keep request fingerprint and safe response metadata for the defined replay window; archive or purge only after expiration. |

Actual retention periods must be reviewed against the merchant's jurisdiction, contract,
and incident-response needs before a production deployment. This demonstration project
does not include an automatic destructive purge job by design.

## Archival safeguards

1. Reconcile balances and journal debit/credit equality before exporting a period.
2. Export with record IDs, timestamps, references, and integrity checksums.
3. Encrypt archival storage and limit access through least-privilege roles.
4. Record the archival operation in the audit trail.
5. Preserve reversal links and idempotency/event references needed for investigations.
6. Test restoration in a non-production environment before deleting any eligible
   operational copy.

Never satisfy retention by deleting ledger records, audit logs, or financial transaction
history from a live database without an approved legal, security, and finance process.
