import { useState, type FormEvent } from 'react'
import { createFloor, updateFloor, useFacilityMutation } from '../../../api/facilities'
import type { Floor, Gender } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { parseNumber } from '../../../lib/forms'
import { ui } from '../../../lib/ui'

export function FloorForm({ buildingId, floor, onDone }: { buildingId: number; floor?: Floor; onDone: () => void }) {
  const [number, setNumber] = useState(floor ? String(floor.number) : '')
  const [preference, setPreference] = useState<Gender | ''>(floor?.genderPreference ?? '')
  const mutation = useFacilityMutation(() => {
    const input = { number: parseNumber(number), genderPreference: preference === '' ? null : preference }
    return floor ? updateFloor(floor.id, input) : createFloor(buildingId, input)
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Floor number">
        <input
          type="number"
          min={1}
          max={99}
          className={ui.input}
          value={number}
          onChange={(event) => setNumber(event.target.value)}
          required
          autoFocus
        />
      </FormField>
      <FormField
        label="Gender preference"
        hint="Only the default filter students see when choosing a room; it never blocks a choice."
      >
        <select
          className={ui.input}
          value={preference}
          onChange={(event) => setPreference(event.target.value as Gender | '')}
        >
          <option value="">No preference</option>
          <option value="MALE">Male</option>
          <option value="FEMALE">Female</option>
        </select>
      </FormField>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={floor ? 'Save' : 'Add floor'} onCancel={onDone} />
    </form>
  )
}
