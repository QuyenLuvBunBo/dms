import { useState, type FormEvent } from 'react'
import { addBed, relabelBed, useFacilityMutation } from '../../../api/facilities'
import type { Bed } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { ui } from '../../../lib/ui'

/** Adds a bed to a room (the code is optional), or relabels `bed`. */
export function BedForm({ roomId, bed, onDone }: { roomId: number; bed?: Bed; onDone: () => void }) {
  const [code, setCode] = useState(bed?.code ?? '')
  const mutation = useFacilityMutation(() =>
    bed ? relabelBed(bed.id, code) : addBed(roomId, code.trim() === '' ? undefined : code),
  )

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Bed code" hint={bed ? undefined : 'Leave empty to use the lowest free number.'}>
        <input
          className={ui.input}
          value={code}
          onChange={(event) => setCode(event.target.value)}
          required={Boolean(bed)}
          maxLength={8}
          autoFocus
        />
      </FormField>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={bed ? 'Save' : 'Add bed'} onCancel={onDone} />
    </form>
  )
}
