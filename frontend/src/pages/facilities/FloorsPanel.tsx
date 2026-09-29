import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { deleteFloor, facilityKeys, listFloors, useFacilityMutation } from '../../api/facilities'
import type { Floor } from '../../api/facilityTypes'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'
import { floorPreferenceLabel } from '../../lib/format'
import { ui } from '../../lib/ui'
import { FloorForm } from './forms/FloorForm'

export function FloorsPanel({ buildingId, isAdmin }: { buildingId: number; isAdmin: boolean }) {
  const floors = useQuery({ queryKey: facilityKeys.floors(buildingId), queryFn: () => listFloors(buildingId) })
  const [editing, setEditing] = useState<Floor | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Floor | null>(null)
  const remove = useFacilityMutation((id: number) => deleteFloor(id))

  return (
    <div className="space-y-4">
      {isAdmin && (
        <div className="flex justify-end">
          <button type="button" className={ui.primaryButton} onClick={() => setEditing('new')}>
            Add floor
          </button>
        </div>
      )}
      <ErrorBanner error={floors.error} />
      <div className={ui.card}>
        <table className="w-full">
          <thead>
            <tr>
              <th className={ui.th}>Floor</th>
              <th className={ui.th}>Gender preference</th>
              <th className={ui.th}>Rooms</th>
              {isAdmin && <th className={ui.th} />}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {floors.data?.map((floor) => (
              <tr key={floor.id}>
                <td className={ui.td}>Floor {floor.number}</td>
                <td className={ui.td}>{floorPreferenceLabel(floor.genderPreference)}</td>
                <td className={ui.td}>{floor.roomCount}</td>
                {isAdmin && (
                  <td className={`${ui.td} space-x-3 text-right`}>
                    <button type="button" className={ui.linkButton} onClick={() => setEditing(floor)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className={ui.linkButton}
                      disabled={floor.roomCount > 0}
                      title={floor.roomCount > 0 ? 'Delete its rooms first' : undefined}
                      onClick={() => {
                        remove.reset()
                        setDeleting(floor)
                      }}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {floors.data?.length === 0 && (
              <tr>
                <td className={ui.td} colSpan={4}>
                  No floors yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <Modal title={editing === 'new' ? 'Add floor' : `Edit floor ${editing.number}`} onClose={() => setEditing(null)}>
          <FloorForm
            buildingId={buildingId}
            floor={editing === 'new' ? undefined : editing}
            onDone={() => setEditing(null)}
          />
        </Modal>
      )}
      {deleting && (
        <ConfirmDialog
          title={`Delete floor ${deleting.number}?`}
          message="Only a floor without rooms can be deleted."
          confirmLabel="Delete floor"
          pending={remove.isPending}
          error={remove.error}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  )
}
