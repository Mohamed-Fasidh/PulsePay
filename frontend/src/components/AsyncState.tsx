import type { ReactNode } from 'react';

export function LoadingState({ label = 'Loading' }: { label?: string }) {
  return <div className="loading-state" role="status" aria-live="polite"><span className="skeleton-line" /><span className="skeleton-line short" /><span className="sr-only">{label}</span></div>;
}

export function EmptyState({ title, detail, action }: { title: string; detail: string; action?: ReactNode }) {
  return <section className="empty-state"><h2>{title}</h2><p>{detail}</p>{action}</section>;
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <section className="error-state" role="alert"><h2>Something went wrong</h2><p>{message}</p>{onRetry && <button type="button" onClick={onRetry}>Try again</button>}</section>;
}
