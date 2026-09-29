import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { deleteBuilding, facilityKeys, listBuildings, useFacilityMutation } from '../../api/facilities'
import type { Building } from '../../api/facilityTypes'
import { useAuth } from '../../auth/useAuth'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { ui } from '../../lib/ui'
import { BuildingForm } from './forms/BuildingForm'

/** ADMIN: every building, with create, edit and delete. Building manager: their own buildings (BR-14). */
export function BuildingListPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const buildings = useQuery({ queryKey: facilityKeys.buildings, queryFn: listBuildings })
  const [editing, setEditing] = useState<Building | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Building | null>(null)
  const remove = useFacilityMutation((id: number) => deleteBuilding(id))

  return (
    <div className="max-w-5xl">
      <PageHeader
        title={isAdmin ? 'Facilities' : 'My buildings'}
        subtitle={isAdmin ? 'Buildings, floors, room types, rooms and beds.' : 'The buildings you manage.'}
        actions={
          isAdmin && (
            <button type="button" className={ui.primaryButton} onClick={() => setEditing('new')}>
              New building
            </button>
          )
        }
      />

      {buildings.isPending && <p className="text-sm text-slate-500">Loading...</p>}
      <ErrorBanner error={buildings.error} />
      {buildings.data && buildings.data.length === 0 && (
        <p className={`${ui.card} p-6 text-sm text-slate-500`}>
          {isAdmin ? 'No buildings yet.' : 'No building is assigned to you yet. Ask the Centre Administrator.'}
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {buildings.data?.map((building) => (
          <div key={building.id} className={`${ui.card} flex flex-col p-5`}>
            <Link to={`/facilities/buildings/${building.id}`} className="group">
              <div className="text-xl font-semibold tracking-tight group-hover:underline">{building.code}</div>
              <div className="text-sm text-slate-500">{building.name}</div>
            </Link>
            <dl className="mt-4 grid grid-cols-3 gap-2 text-center">
              <Stat label="Floors" value={building.floorCount} />
              <Stat label="Rooms" value={building.roomCount} />
              <Stat label="Beds" value={building.bedCount} />
            </dl>
            <p className="mt-3 text-xs text-slate-500">
              {building.occupiedBedCount} of {building.bedCount} beds occupied
            </p>
            <p className="mt-1 text-xs text-slate-500">
              Managers:{' '}
              {building.managers.length > 0
                ? building.managers.map((manager) => manager.fullName).join(', ')
                : 'none assigned'}
            </p>
            {isAdmin && (
              <div className="mt-4 flex gap-2 border-t border-slate-100 pt-4">
                <button type="button" className={ui.secondaryButton} onClick={() => setEditing(building)}>
                  Edit
                </button>
                <button
                  type="button"
                  className={ui.dangerButton}
                  onClick={() => {
                    remove.reset()
                    setDeleting(building)
                  }}
                >
                  Delete
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      {editing && (
        <Modal title={editing === 'new' ? 'New building' : `Edit ${editing.code}`} onClose={() => setEditing(null)}>
          <BuildingForm building={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.code}?`}
          message="A building can be deleted only when it has no floors and no room types left. Its manager links are removed with it."
          confirmLabel="Delete building"
          pending={remove.isPending}
          error={remove.error}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md bg-slate-50 py-2">
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  )
}
