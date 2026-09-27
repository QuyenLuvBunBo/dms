import { useState, type FormEvent } from 'react'
import { createRoom, updateRoom, useFacilityMutation } from '../../../api/facilities'
import type { Floor, RoomDetail, RoomType } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { formatVnd } from '../../../lib/format'
import { ui } from '../../../lib/ui'

interface RoomFormProps {
  floors: Floor[]
  roomTypes: RoomType[]
  /** Edits this room (its floor stays); without it the form creates a room. */
  room?: RoomDetail
  onDone: () => void
}

export function RoomForm({ floors, roomTypes, room, onDone }: RoomFormProps) {
  const [floorId, setFloorId] = useState(room ? String(room.floorId) : floors[0] ? String(floors[0].id) : '')
  const [code, setCode] = useState(room?.code ?? '')
  const [roomTypeId, setRoomTypeId] = useState(
    room ? String(room.roomType.id) : roomTypes[0] ? String(roomTypes[0].id) : '',
  )
  const selectedType = roomTypes.find((type) => String(type.id) === roomTypeId)
  const mutation = useFacilityMutation(() => {
    const input = { code, roomTypeId: Number(roomTypeId) }
    return room ? updateRoom(room.id, input) : createRoom(Number(floorId), input)
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  if (!room && (floors.length === 0 || roomTypes.length === 0)) {
    return (
      <div className="space-y-4 text-sm text-slate-700">
        <p>Add at least one floor and one room type to this building first.</p>
        <div className="flex justify-end">
          <button type="button" className={ui.secondaryButton} onClick={onDone}>
            Close
          </button>
        </div>
      </div>
    )
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {!room && (
        <FormField label="Floor">
          <select className={ui.input} value={floorId} onChange={(event) => setFloorId(event.target.value)}>
            {floors.map((floor) => (
              <option key={floor.id} value={floor.id}>
                Floor {floor.number}
              </option>
            ))}
          </select>
        </FormField>
      )}
      <FormField label="Room code" hint="Unique in the building, e.g. 301">
        <input
          className={ui.input}
          value={code}
          onChange={(event) => setCode(event.target.value)}
          required
          maxLength={16}
          autoFocus
        />
      </FormField>
      <FormField
        label="Room type"
        hint={
          selectedType && !room
            ? `The room gets ${selectedType.capacity} beds, one per place.`
            : room
              ? `The beds stay as they are (${room.beds.length}); the new type must allow at least that many.`
              : undefined
        }
      >
        <select className={ui.input} value={roomTypeId} onChange={(event) => setRoomTypeId(event.target.value)}>
          {roomTypes.map((type) => (
            <option key={type.id} value={type.id}>
              {type.name}: {type.capacity} beds, {formatVnd(type.monthlyRent)}/month
            </option>
          ))}
        </select>
      </FormField>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={room ? 'Save' : 'Create room'} onCancel={onDone} />
    </form>
  )
}
