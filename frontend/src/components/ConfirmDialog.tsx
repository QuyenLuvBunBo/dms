import { ui } from '../lib/ui'
import { ErrorBanner } from './ErrorBanner'
import { Modal } from './Modal'

interface ConfirmDialogProps {
  title: string
  message: string
  confirmLabel: string
  pending: boolean
  error: unknown
  onConfirm: () => void
  onClose: () => void
}

/** Asks before a destructive action; the backend's refusal (e.g. 409) is shown in place. */
export function ConfirmDialog({ title, message, confirmLabel, pending, error, onConfirm, onClose }: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="text-sm text-slate-700">{message}</p>
      <div className="mt-4 space-y-4">
        <ErrorBanner error={error} />
        <div className="flex justify-end gap-2">
          <button type="button" className={ui.secondaryButton} onClick={onClose}>
            Cancel
          </button>
          <button type="button" className={ui.dangerButton} disabled={pending} onClick={onConfirm}>
            {pending ? 'Working...' : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
