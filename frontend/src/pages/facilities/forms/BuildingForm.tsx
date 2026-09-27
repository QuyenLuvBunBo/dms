import { useState, type FormEvent } from 'react'
import { createBuilding, updateBuilding, useFacilityMutation } from '../../../api/facilities'
import type { Building } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { ui } from '../../../lib/ui'

/** Creates a building, or edits one when `building` is given. */
export function BuildingForm({ building, onDone }: { building?: Building; onDone: () => void }) {
  const [code, setCode] = useState(building?.code ?? '')
  const [name, setName] = useState(building?.name ?? '')
  const mutation = useFacilityMutation(() =>
    building ? updateBuilding(building.id, { code, name }) : createBuilding({ code, name }),
  )

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Code" hint="Short and unique, e.g. B6">
        <input
          className={ui.input}
          value={code}
          onChange={(event) => setCode(event.target.value)}
          required
          maxLength={16}
          autoFocus
        />
      </FormField>
      <FormField label="Name">
        <input
          className={ui.input}
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
          maxLength={128}
        />
      </FormField>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={building ? 'Save' : 'Create building'} onCancel={onDone} />
    </form>
  )
}
