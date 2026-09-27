import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { deleteRoomType, facilityKeys, listRoomTypes, useFacilityMutation } from '../../api/facilities'
import type { RoomType } from '../../api/facilityTypes'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'
import { formatVnd } from '../../lib/format'
import { ui } from '../../lib/ui'
import { RoomTypeForm } from './forms/RoomTypeForm'

export function RoomTypesPanel({ buildingId, isAdmin }: { buildingId: number; isAdmin: boolean }) {
  const roomTypes = useQuery({
    queryKey: facilityKeys.roomTypes(buildingId),
    queryFn: () => listRoomTypes(buildingId),
  })
  const [editing, setEditing] = useState<RoomType | 'new' | null>(null)
  const [deleting, setDeleting] = useState<RoomType | null>(null)
  const remove = useFacilityMutation((id: number) => deleteRoomType(id))

  return (
    <div className="space-y-4">
      {isAdmin && (
        <div className="flex justify-end">
          <button type="button" className={ui.primaryButton} onClick={() => setEditing('new')}>
            Add room type
          </button>
        </div>
      )}
      <ErrorBanner error={roomTypes.error} />
      <div className={`${ui.card} overflow-x-auto`}>
        <table className="w-full">
          <thead>
            <tr>
              <th className={ui.th}>Name</th>
              <th className={ui.th}>Capacity</th>
              <th className={ui.th}>Area</th>
              <th className={ui.th}>Air con</th>
              <th className={ui.th}>Water heater</th>
              <th className={ui.th}>Bathrooms</th>
              <th className={ui.th}>Rent / month</th>
              <th className={ui.th}>Rooms</th>
              {isAdmin && <th className={ui.th} />}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {roomTypes.data?.map((type) => (
              <tr key={type.id}>
                <td className={`${ui.td} font-medium text-slate-900`}>{type.name}</td>
                <td className={ui.td}>{type.capacity} beds</td>
                <td className={ui.td}>{type.areaM2} m²</td>
                <td className={ui.td}>{type.hasAirConditioning ? 'Yes' : 'No'}</td>
                <td className={ui.td}>{type.hasWaterHeater ? 'Yes' : 'No'}</td>
                <td className={ui.td}>{type.bathrooms}</td>
                <td className={ui.td}>{formatVnd(type.monthlyRent)}</td>
                <td className={ui.td}>{type.roomCount}</td>
                {isAdmin && (
                  <td className={`${ui.td} space-x-3 text-right whitespace-nowrap`}>
                    <button type="button" className={ui.linkButton} onClick={() => setEditing(type)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className={ui.linkButton}
                      disabled={type.roomCount > 0}
                      title={type.roomCount > 0 ? 'Rooms use this type' : undefined}
                      onClick={() => {
                        remove.reset()
                        setDeleting(type)
                      }}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {roomTypes.data?.length === 0 && (
              <tr>
                <td className={ui.td} colSpan={9}>
                  No room types yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <Modal title={editing === 'new' ? 'Add room type' : `Edit ${editing.name}`} onClose={() => setEditing(null)}>
          <RoomTypeForm
            buildingId={buildingId}
            roomType={editing === 'new' ? undefined : editing}
            onDone={() => setEditing(null)}
          />
        </Modal>
      )}
      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.name}?`}
          message="Only a room type that no room uses can be deleted."
          confirmLabel="Delete room type"
          pending={remove.isPending}
          error={remove.error}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  )
}
