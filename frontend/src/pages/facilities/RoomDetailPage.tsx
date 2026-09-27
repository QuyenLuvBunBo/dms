import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import {
  deleteAsset,
  deleteRoom,
  facilityKeys,
  getRoom,
  listFloors,
  listRoomTypes,
  removeBed,
  useFacilityMutation,
} from '../../api/facilities'
import type { Bed, RoomAsset, RoomAssetStatus } from '../../api/facilityTypes'
import { useAuth } from '../../auth/useAuth'
import { Badge, type BadgeTone } from '../../components/Badge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import {
  amenitiesLabel,
  ASSET_STATUS_LABELS,
  floorPreferenceLabel,
  formatVnd,
  roomGenderLabel,
} from '../../lib/format'
import { ui } from '../../lib/ui'
import { NotFoundPage } from '../NotFoundPage'
import { AssetForm } from './forms/AssetForm'
import { BedForm } from './forms/BedForm'
import { RoomForm } from './forms/RoomForm'

const ASSET_TONES: Record<RoomAssetStatus, BadgeTone> = { GOOD: 'green', DAMAGED: 'amber', MISSING: 'red' }

type Dialog =
  | { kind: 'edit-room' }
  | { kind: 'delete-room' }
  | { kind: 'bed'; bed?: Bed }
  | { kind: 'remove-bed'; bed: Bed }
  | { kind: 'asset'; asset?: RoomAsset }
  | { kind: 'delete-asset'; asset: RoomAsset }

/**
 * One room: its type, beds and assets. ADMIN manages beds and the room itself; ADMIN and the
 * building's manager manage assets. A room of another building answers 404 (BR-14).
 */
export function RoomDetailPage() {
  const roomId = Number(useParams().roomId)
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [dialog, setDialog] = useState<Dialog | null>(null)
  const room = useQuery({
    queryKey: facilityKeys.room(roomId),
    queryFn: () => getRoom(roomId),
    enabled: Number.isInteger(roomId),
  })
  const buildingId = room.data?.buildingId ?? 0
  const floors = useQuery({
    queryKey: facilityKeys.floors(buildingId),
    queryFn: () => listFloors(buildingId),
    enabled: isAdmin && buildingId > 0,
  })
  const roomTypes = useQuery({
    queryKey: facilityKeys.roomTypes(buildingId),
    queryFn: () => listRoomTypes(buildingId),
    enabled: isAdmin && buildingId > 0,
  })
  const queryClient = useQueryClient()
  // Leaves the page before refreshing, so the deleted room is not refetched into a 404 first.
  const removeRoom = useMutation({
    mutationFn: () => deleteRoom(roomId),
    onSuccess: () => {
      navigate(`/facilities/buildings/${buildingId}`, { replace: true })
      queryClient.removeQueries({ queryKey: facilityKeys.room(roomId) })
      return queryClient.invalidateQueries({ queryKey: facilityKeys.buildings })
    },
  })
  const removeOneBed = useFacilityMutation((bedId: number) => removeBed(bedId))
  const removeAsset = useFacilityMutation((assetId: number) => deleteAsset(assetId))

  if (!Number.isInteger(roomId) || (room.error instanceof ApiError && room.error.status === 404)) {
    return <NotFoundPage />
  }
  if (room.error) {
    return <ErrorBanner error={room.error} />
  }
  if (!room.data) {
    return <p className="text-sm text-slate-500">Loading...</p>
  }
  const data = room.data
  const type = data.roomType
  const occupiedBeds = data.beds.filter((bed) => bed.occupied).length
  const full = data.beds.length >= type.capacity
  const close = () => setDialog(null)
  const open = (next: Dialog) => {
    removeRoom.reset()
    removeOneBed.reset()
    removeAsset.reset()
    setDialog(next)
  }

  return (
    <div className="max-w-5xl space-y-6">
      <PageHeader
        title={`Room ${data.code}`}
        back={{ to: `/facilities/buildings/${data.buildingId}`, label: `Building ${data.buildingCode}` }}
        subtitle={`${data.buildingCode}, floor ${data.floorNumber} · ${floorPreferenceLabel(data.floorGenderPreference)}`}
        actions={
          isAdmin && (
            <>
              <button
                type="button"
                className={ui.secondaryButton}
                disabled={!floors.data || !roomTypes.data}
                onClick={() => open({ kind: 'edit-room' })}
              >
                Edit room
              </button>
              <button
                type="button"
                className={ui.dangerButton}
                disabled={occupiedBeds > 0}
                title={occupiedBeds > 0 ? 'A room with occupants cannot be deleted' : undefined}
                onClick={() => open({ kind: 'delete-room' })}
              >
                Delete room
              </button>
            </>
          )
        }
      />

      <section className={`${ui.card} grid gap-4 p-5 sm:grid-cols-4`}>
        <Info label="Room type" value={type.name} />
        <Info label="Rent" value={`${formatVnd(type.monthlyRent)} / month`} />
        <Info label="Amenities" value={`${amenitiesLabel(type)}, ${type.areaM2} m²`} />
        <Info label="Room gender" value={roomGenderLabel(data.gender)} />
      </section>

      <section className={ui.card}>
        <header className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <h2 className="text-sm font-semibold">
            Beds · {data.beds.length} of capacity {type.capacity}, {occupiedBeds} occupied
          </h2>
          {isAdmin && (
            <button
              type="button"
              className={ui.secondaryButton}
              disabled={full}
              title={full ? 'The room already has as many beds as its capacity' : undefined}
              onClick={() => open({ kind: 'bed' })}
            >
              Add bed
            </button>
          )}
        </header>
        <ul className="grid gap-2 p-4 sm:grid-cols-4 lg:grid-cols-6">
          {data.beds.map((bed) => (
            <li key={bed.id} className="rounded-md border border-slate-200 p-3 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-medium">Bed {bed.code}</span>
                <Badge tone={bed.occupied ? 'amber' : 'green'}>{bed.occupied ? 'Occupied' : 'Free'}</Badge>
              </div>
              {isAdmin && (
                <div className="mt-2 space-x-3">
                  <button type="button" className={ui.linkButton} onClick={() => open({ kind: 'bed', bed })}>
                    Relabel
                  </button>
                  <button
                    type="button"
                    className={ui.linkButton}
                    disabled={bed.occupied}
                    title={bed.occupied ? 'An occupied bed cannot be removed' : undefined}
                    onClick={() => open({ kind: 'remove-bed', bed })}
                  >
                    Remove
                  </button>
                </div>
              )}
            </li>
          ))}
          {data.beds.length === 0 && <li className="text-sm text-slate-500">No beds.</li>}
        </ul>
      </section>

      <section className={ui.card}>
        <header className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <h2 className="text-sm font-semibold">Assets</h2>
          <button type="button" className={ui.secondaryButton} onClick={() => open({ kind: 'asset' })}>
            Add item
          </button>
        </header>
        <table className="w-full">
          <tbody className="divide-y divide-slate-100">
            {data.assets.map((asset) => (
              <tr key={asset.id}>
                <td className={ui.td}>{asset.name}</td>
                <td className={ui.td}>
                  <Badge tone={ASSET_TONES[asset.status]}>{ASSET_STATUS_LABELS[asset.status]}</Badge>
                </td>
                <td className={`${ui.td} space-x-3 text-right`}>
                  <button type="button" className={ui.linkButton} onClick={() => open({ kind: 'asset', asset })}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className={ui.linkButton}
                    onClick={() => open({ kind: 'delete-asset', asset })}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
            {data.assets.length === 0 && (
              <tr>
                <td className={ui.td}>No assets recorded.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {dialog?.kind === 'edit-room' && floors.data && roomTypes.data && (
        <Modal title={`Edit room ${data.code}`} onClose={close}>
          <RoomForm floors={floors.data} roomTypes={roomTypes.data} room={data} onDone={close} />
        </Modal>
      )}
      {dialog?.kind === 'delete-room' && (
        <ConfirmDialog
          title={`Delete room ${data.code}?`}
          message="The room is deleted with its beds and assets. A room with occupants cannot be deleted."
          confirmLabel="Delete room"
          pending={removeRoom.isPending}
          error={removeRoom.error}
          onConfirm={() => removeRoom.mutate()}
          onClose={close}
        />
      )}
      {dialog?.kind === 'bed' && (
        <Modal title={dialog.bed ? `Relabel bed ${dialog.bed.code}` : 'Add bed'} onClose={close}>
          <BedForm roomId={roomId} bed={dialog.bed} onDone={close} />
        </Modal>
      )}
      {dialog?.kind === 'remove-bed' && (
        <ConfirmDialog
          title={`Remove bed ${dialog.bed.code}?`}
          message="An occupied bed cannot be removed."
          confirmLabel="Remove bed"
          pending={removeOneBed.isPending}
          error={removeOneBed.error}
          onConfirm={() => removeOneBed.mutate(dialog.bed.id, { onSuccess: close })}
          onClose={close}
        />
      )}
      {dialog?.kind === 'asset' && (
        <Modal title={dialog.asset ? `Edit ${dialog.asset.name}` : 'Add item'} onClose={close}>
          <AssetForm roomId={roomId} asset={dialog.asset} onDone={close} />
        </Modal>
      )}
      {dialog?.kind === 'delete-asset' && (
        <ConfirmDialog
          title={`Remove ${dialog.asset.name}?`}
          message="The item is removed from the room's inventory."
          confirmLabel="Remove item"
          pending={removeAsset.isPending}
          error={removeAsset.error}
          onConfirm={() => removeAsset.mutate(dialog.asset.id, { onSuccess: close })}
          onClose={close}
        />
      )}
    </div>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-0.5 text-sm font-medium text-slate-900">{value}</div>
    </div>
  )
}
