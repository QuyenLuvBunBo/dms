import { ui } from '../lib/ui'

export function FormActions({
  pending,
  submitLabel,
  onCancel,
}: {
  pending: boolean
  submitLabel: string
  onCancel: () => void
}) {
  return (
    <div className="flex justify-end gap-2 pt-2">
      <button type="button" className={ui.secondaryButton} onClick={onCancel}>
        Cancel
      </button>
      <button type="submit" className={ui.primaryButton} disabled={pending}>
        {pending ? 'Saving...' : submitLabel}
      </button>
    </div>
  )
}
