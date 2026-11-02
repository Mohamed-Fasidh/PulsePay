import { useState } from 'react';
import type { FormEvent } from 'react';

export const supportedTestEvents = [
  'payment.succeeded',
  'payment.failed',
  'payment.captured',
  'refund.created',
  'refund.succeeded',
  'refund.failed',
] as const;

export type TestEventType = (typeof supportedTestEvents)[number];

export function testEventPayload(eventType: TestEventType, paymentIntentId: string) {
  return { eventType, ...(paymentIntentId.trim() ? { paymentIntentId: paymentIntentId.trim() } : {}) };
}

type Props = {
  accessToken: string;
  apiBaseUrl?: string;
  onQueued?: (eventId: string) => void;
};

/** A safe developer-tool control: it creates a synthetic webhook event, never a payment. */
export function TestEventSimulator({ accessToken, apiBaseUrl = '/api/v1', onQueued }: Props) {
  const [eventType, setEventType] = useState<TestEventType>('payment.succeeded');
  const [paymentIntentId, setPaymentIntentId] = useState('');
  const [state, setState] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');
  const [message, setMessage] = useState('');

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState('submitting');
    setMessage('');
    try {
      const response = await fetch(`${apiBaseUrl}/developer/test-events`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(testEventPayload(eventType, paymentIntentId)),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body?.error?.message || 'Unable to queue the test event.');
      setState('success');
      setMessage(`Queued ${eventType} (${body.event_id}).`);
      onQueued?.(body.event_id);
    } catch (error) {
      setState('error');
      setMessage(error instanceof Error ? error.message : 'Unable to queue the test event.');
    }
  }

  return (
    <section className="panel" aria-labelledby="test-event-heading">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Test mode</p>
          <h2 id="test-event-heading">Webhook event simulator</h2>
        </div>
      </div>
      <p className="muted">Queues a synthetic event through the normal outbox. It never charges a card or creates a ledger movement.</p>
      <form className="form-grid" onSubmit={submit}>
        <label>
          Event type
          <select value={eventType} onChange={(event) => setEventType(event.target.value as TestEventType)}>
            {supportedTestEvents.map((event) => <option key={event} value={event}>{event}</option>)}
          </select>
        </label>
        <label>
          Payment intent ID <span className="muted">(optional)</span>
          <input value={paymentIntentId} onChange={(event) => setPaymentIntentId(event.target.value)} placeholder="pi_..." />
        </label>
        <button type="submit" disabled={state === 'submitting'}>{state === 'submitting' ? 'Queueing…' : 'Send test event'}</button>
      </form>
      {state !== 'idle' && <p role={state === 'error' ? 'alert' : 'status'} className={state === 'error' ? 'error' : 'success'}>{message}</p>}
    </section>
  );
}
