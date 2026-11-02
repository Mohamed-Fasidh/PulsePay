import type { ReactNode } from 'react';

type Props = {
  open: boolean;
  title: string;
  detail: ReactNode;
  confirmLabel: string;
  busy?: boolean;
  destructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/** Explicit confirmation for non-idempotent or destructive dashboard actions. */
export function ConfirmDialog({ open, title, detail, confirmLabel, busy = false, destructive = false, onConfirm, onCancel }: Props) {
  if (!open) return null;
  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={onCancel}>
      <section className="dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="confirm-dialog-title">{title}</h2>
        <div className="dialog-detail">{detail}</div>
        <div className="dialog-actions">
          <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
          <button type="button" className={destructive ? 'danger' : undefined} onClick={onConfirm} disabled={busy}>{busy ? 'Working…' : confirmLabel}</button>
        </div>
      </section>
    </div>
  );
}
