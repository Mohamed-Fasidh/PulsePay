import { describe, expect, it } from 'vitest';
import { supportedTestEvents, testEventPayload } from './TestEventSimulator';

describe('test event simulator payloads', () => {
  it('only exposes the supported synthetic event types', () => {
    expect(supportedTestEvents).toContain('payment.succeeded');
    expect(supportedTestEvents).not.toContain('payment.created');
  });

  it('omits an empty optional payment intent reference', () => {
    expect(testEventPayload('refund.failed', '  ')).toEqual({ eventType: 'refund.failed' });
  });

  it('trims a provided payment intent reference', () => {
    expect(testEventPayload('payment.captured', ' pi_demo ')).toEqual({ eventType: 'payment.captured', paymentIntentId: 'pi_demo' });
  });
});
