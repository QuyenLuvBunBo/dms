import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { facilityKeys, listFloors, listRooms, listRoomTypes } from '../../api/facilities'
import type { RoomSummary } from '../../api/facilityTypes'
import { Badge } from '../../components/Badge'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'
import { amenitiesLabel, floorPreferenceLabel, formatVnd, roomGenderLabel } from '../../lib/format'
import { ui } from '../../lib/ui'
import { RoomForm } from './forms/RoomForm'

/** The building's rooms, grouped by floor. */
export function RoomsPanel({ buildingId, isAdmin }: { buildingId: number; isAdmin: boolean }) {
  const rooms = useQuery({ queryKey: facilityKeys.rooms(buildingId), queryFn: () => listRooms(buildingId) })
  const floors = useQuery({ queryKey: facilityKeys.floors(buildingId), queryFn: () => listFloors(buildingId) })
  const roomTypes = useQuery({
    queryKey: facilityKeys.roomTypes(buildingId),
    queryFn: () => listRoomTypes(buildingId),
    enabled: isAdmin,
  })
  const [creating, setCreating] = useState(false)

  const byFloor = new Map<number, RoomSummary[]>()
  for (const room of rooms.data ?? []) {
    byFloor.set(room.floorNumber, [...(byFloor.get(room.floorNumber) ?? []), room])
  }

  return (
    <div className="space-y-6">
      {isAdmin && (
        <div className="flex justify-end">
          <button
            type="button"
            className={ui.primaryButton}
            disabled={!floors.data || !roomTypes.data}
            onClick={() => setCreating(true)}
          >
            New room
          </button>
        </div>
      )}
      <ErrorBanner error={rooms.error ?? floors.error} />
      {rooms.isPending && <p className="text-sm text-slate-500">Loading...</p>}
      {rooms.data && rooms.data.length === 0 && (
        <p className={`${ui.card} p-6 text-sm text-slate-500`}>No rooms in this building yet.</p>
      )}

      {[...byFloor.entries()].map(([floorNumber, floorRooms]) => (
        <section key={floorNumber} className={ui.card}>
          <header className="flex items-center gap-3 border-b border-slate-100 px-4 py-3">
            <h2 className="text-sm font-semibold">Floor {floorNumber}</h2>
            <Badge tone={genderTone(floorRooms[0].floorGenderPreference)}>
              {floorPreferenceLabel(floorRooms[0].floorGenderPreference)}
            </Badge>
          </header>
          <table className="w-full">
            <thead>
              <tr>
                <th className={ui.th}>Room</th>
                <th className={ui.th}>Room type</th>
                <th className={ui.th}>Rent / month</th>
                <th className={ui.th}>Amenities</th>
                <th className={ui.th}>Beds</th>
                <th className={ui.th}>Room gender</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {floorRooms.map((room) => (
                <tr key={room.id}>
                  <td className={ui.td}>
                    <Link to={`/facilities/rooms/${room.id}`} className="font-medium text-slate-900 hover:underline">
                      {room.code}
                    </Link>
                  </td>
                  <td className={ui.td}>{room.roomType.name}</td>
                  <td className={ui.td}>{formatVnd(room.roomType.monthlyRent)}</td>
                  <td className={ui.td}>{amenitiesLabel(room.roomType)}</td>
                  <td className={ui.td}>
                    {room.bedCount - room.occupiedBedCount} free of {room.bedCount}
                    {room.bedCount < room.roomType.capacity && (
                      <span className="text-slate-400"> (capacity {room.roomType.capacity})</span>
                    )}
                  </td>
                  <td className={ui.td}>
                    <Badge tone={room.gender ? genderTone(room.gender) : 'neutral'}>{roomGenderLabel(room.gender)}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}

      {creating && floors.data && roomTypes.data && (
        <Modal title="New room" onClose={() => setCreating(false)}>
          <RoomForm floors={floors.data} roomTypes={roomTypes.data} onDone={() => setCreating(false)} />
        </Modal>
      )}
    </div>
  )
}

function genderTone(gender: RoomSummary['gender']) {
  if (gender === 'MALE') {
    return 'blue' as const
  }
  return gender === 'FEMALE' ? ('pink' as const) : ('neutral' as const)
}
