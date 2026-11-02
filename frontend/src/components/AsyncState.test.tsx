import { describe, expect, it } from 'vitest';
import { EmptyState, ErrorState, LoadingState } from './AsyncState';

describe('dashboard async states', () => {
  it('exports loading, empty, and error components for data-driven pages', () => {
    expect(LoadingState).toBeTypeOf('function');
    expect(EmptyState).toBeTypeOf('function');
    expect(ErrorState).toBeTypeOf('function');
  });
});
