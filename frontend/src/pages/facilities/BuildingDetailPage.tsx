import { useQuery } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import { facilityKeys, getBuilding } from '../../api/facilities'
import { useAuth } from '../../auth/useAuth'
import { ErrorBanner } from '../../components/ErrorBanner'
import { PageHeader } from '../../components/PageHeader'
import { NotFoundPage } from '../NotFoundPage'
import { FloorsPanel } from './FloorsPanel'
import { RoomsPanel } from './RoomsPanel'
import { RoomTypesPanel } from './RoomTypesPanel'

const TABS = [
  { id: 'rooms', label: 'Rooms' },
  { id: 'floors', label: 'Floors' },
  { id: 'room-types', label: 'Room types' },
] as const

type TabId = (typeof TABS)[number]['id']

export function BuildingDetailPage() {
  const buildingId = Number(useParams().buildingId)
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [searchParams, setSearchParams] = useSearchParams()
  const tab: TabId = TABS.find((candidate) => candidate.id === searchParams.get('tab'))?.id ?? 'rooms'
  const building = useQuery({
    queryKey: facilityKeys.building(buildingId),
    queryFn: () => getBuilding(buildingId),
    enabled: Number.isInteger(buildingId),
  })

  // Another building's id answers 404 exactly like an id that does not exist (BR-14).
  if (!Number.isInteger(buildingId) || (building.error instanceof ApiError && building.error.status === 404)) {
    return <NotFoundPage />
  }
  if (building.error) {
    return <ErrorBanner error={building.error} />
  }
  if (!building.data) {
    return <p className="text-sm text-slate-500">Loading...</p>
  }
  const data = building.data

  return (
    <div className="max-w-6xl">
      <PageHeader
        title={`${data.code} · ${data.name}`}
        back={{ to: '/facilities', label: isAdmin ? 'Facilities' : 'My buildings' }}
        subtitle={
          <>
            {data.floorCount} floors, {data.roomCount} rooms, {data.bedCount} beds ({data.occupiedBedCount}{' '}
            occupied). Managers:{' '}
            {data.managers.length > 0 ? data.managers.map((manager) => manager.fullName).join(', ') : 'none assigned'}
          </>
        }
      />

      <div className="mb-4 flex gap-1 border-b border-slate-200" role="tablist">
        {TABS.map((candidate) => (
          <button
            key={candidate.id}
            type="button"
            role="tab"
            aria-selected={tab === candidate.id}
            onClick={() => setSearchParams(candidate.id === 'rooms' ? {} : { tab: candidate.id }, { replace: true })}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
              tab === candidate.id
                ? 'border-slate-900 text-slate-900'
                : 'border-transparent text-slate-500 hover:text-slate-900'
            }`}
          >
            {candidate.label}
          </button>
        ))}
      </div>

      {tab === 'rooms' && <RoomsPanel buildingId={buildingId} isAdmin={isAdmin} />}
      {tab === 'floors' && <FloorsPanel buildingId={buildingId} isAdmin={isAdmin} />}
      {tab === 'room-types' && <RoomTypesPanel buildingId={buildingId} isAdmin={isAdmin} />}
    </div>
  )
}
