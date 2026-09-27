import { useState, type FormEvent } from 'react'
import { createRoomType, updateRoomType, useFacilityMutation } from '../../../api/facilities'
import type { RoomType } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { parseNumber } from '../../../lib/forms'
import { ui } from '../../../lib/ui'

export function RoomTypeForm({
  buildingId,
  roomType,
  onDone,
}: {
  buildingId: number
  roomType?: RoomType
  onDone: () => void
}) {
  const [name, setName] = useState(roomType?.name ?? '')
  const [capacity, setCapacity] = useState(roomType ? String(roomType.capacity) : '')
  const [area, setArea] = useState(roomType ? String(roomType.areaM2) : '')
  const [airConditioning, setAirConditioning] = useState(roomType?.hasAirConditioning ?? true)
  const [waterHeater, setWaterHeater] = useState(roomType?.hasWaterHeater ?? true)
  const [bathrooms, setBathrooms] = useState(roomType ? String(roomType.bathrooms) : '1')
  const [rent, setRent] = useState(roomType ? String(roomType.monthlyRent) : '')
  const mutation = useFacilityMutation(() => {
    const input = {
      name,
      capacity: parseNumber(capacity),
      areaM2: parseNumber(area),
      hasAirConditioning: airConditioning,
      hasWaterHeater: waterHeater,
      bathrooms: parseNumber(bathrooms),
      monthlyRent: parseNumber(rent),
    }
    return roomType ? updateRoomType(roomType.id, input) : createRoomType(buildingId, input)
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Name">
        <input
          className={ui.input}
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="8-student room"
          required
          maxLength={64}
          autoFocus
        />
      </FormField>
      <div className="grid grid-cols-2 gap-4">
        <FormField
          label="Capacity (beds)"
          hint={roomType && roomType.roomCount > 0 ? 'Cannot drop below the beds a room already has.' : undefined}
        >
          <input
            type="number"
            min={1}
            max={50}
            className={ui.input}
            value={capacity}
            onChange={(event) => setCapacity(event.target.value)}
            required
          />
        </FormField>
        <FormField label="Area (m²)">
          <input
            type="number"
            min={0.01}
            step={0.01}
            className={ui.input}
            value={area}
            onChange={(event) => setArea(event.target.value)}
            required
          />
        </FormField>
        <FormField label="Bathrooms">
          <input
            type="number"
            min={0}
            max={20}
            className={ui.input}
            value={bathrooms}
            onChange={(event) => setBathrooms(event.target.value)}
            required
          />
        </FormField>
        <FormField label="Monthly rent (VND per student)">
          <input
            type="number"
            min={0}
            step={1000}
            className={ui.input}
            value={rent}
            onChange={(event) => setRent(event.target.value)}
            required
          />
        </FormField>
      </div>
      <div className="flex gap-6 text-sm">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={airConditioning}
            onChange={(event) => setAirConditioning(event.target.checked)}
          />
          Air conditioning
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={waterHeater} onChange={(event) => setWaterHeater(event.target.checked)} />
          Water heater
        </label>
      </div>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={roomType ? 'Save' : 'Add room type'} onCancel={onDone} />
    </form>
  )
}
