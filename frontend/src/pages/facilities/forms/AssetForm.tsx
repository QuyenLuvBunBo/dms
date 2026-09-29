import { useState, type FormEvent } from 'react'
import { createAsset, updateAsset, useFacilityMutation } from '../../../api/facilities'
import { ROOM_ASSET_STATUSES, type RoomAsset, type RoomAssetStatus } from '../../../api/facilityTypes'
import { ErrorBanner } from '../../../components/ErrorBanner'
import { FormActions } from '../../../components/FormActions'
import { FormField } from '../../../components/FormField'
import { ASSET_STATUS_LABELS } from '../../../lib/format'
import { ui } from '../../../lib/ui'

export function AssetForm({ roomId, asset, onDone }: { roomId: number; asset?: RoomAsset; onDone: () => void }) {
  const [name, setName] = useState(asset?.name ?? '')
  const [status, setStatus] = useState<RoomAssetStatus>(asset?.status ?? 'GOOD')
  const mutation = useFacilityMutation(() =>
    asset ? updateAsset(asset.id, { name, status }) : createAsset(roomId, { name, status }),
  )

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate(undefined, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Item">
        <input
          className={ui.input}
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Air conditioner"
          required
          maxLength={128}
          autoFocus
        />
      </FormField>
      <FormField label="Status">
        <select
          className={ui.input}
          value={status}
          onChange={(event) => setStatus(event.target.value as RoomAssetStatus)}
        >
          {ROOM_ASSET_STATUSES.map((value) => (
            <option key={value} value={value}>
              {ASSET_STATUS_LABELS[value]}
            </option>
          ))}
        </select>
      </FormField>
      <ErrorBanner error={mutation.error} />
      <FormActions pending={mutation.isPending} submitLabel={asset ? 'Save' : 'Add item'} onCancel={onDone} />
    </form>
  )
}
