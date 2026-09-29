import { useQuery } from '@tanstack/react-query'
import {
  assignManager,
  facilityKeys,
  listBuildings,
  listManagers,
  unassignManager,
  useFacilityMutation,
} from '../../api/facilities'
import { ErrorBanner } from '../../components/ErrorBanner'
import { PageHeader } from '../../components/PageHeader'
import { ui } from '../../lib/ui'

interface Toggle {
  buildingId: number
  userId: number
  linked: boolean
}

/**
 * ADMIN links building managers to buildings (BR-14): one row per BUILDING_MANAGER account, one
 * checkbox per building. A manager may have several buildings. Accounts come from the seed for now.
 */
export function BuildingManagersPage() {
  const managers = useQuery({ queryKey: facilityKeys.managers, queryFn: listManagers })
  const buildings = useQuery({ queryKey: facilityKeys.buildings, queryFn: listBuildings })
  const toggle = useFacilityMutation(({ buildingId, userId, linked }: Toggle) =>
    linked ? unassignManager(buildingId, userId) : assignManager(buildingId, userId),
  )

  return (
    <div className="max-w-6xl">
      <PageHeader
        title="Building managers"
        subtitle="Tick the buildings each manager is responsible for. A manager sees and changes only those buildings."
      />
      <div className="mb-4 space-y-2">
        <ErrorBanner error={managers.error ?? buildings.error} />
        <ErrorBanner error={toggle.error} />
      </div>
      {(managers.isPending || buildings.isPending) && <p className="text-sm text-slate-500">Loading...</p>}
      {managers.data && buildings.data && (
        <div className={`${ui.card} overflow-x-auto`}>
          <table className="w-full">
            <thead>
              <tr>
                <th className={ui.th}>Manager</th>
                {buildings.data.map((building) => (
                  <th key={building.id} className={`${ui.th} text-center`}>
                    {building.code}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {managers.data.map((manager) => (
                <tr key={manager.userId}>
                  <td className={ui.td}>
                    <div className="font-medium text-slate-900">{manager.fullName}</div>
                    <div className="text-xs text-slate-500">{manager.username}</div>
                  </td>
                  {buildings.data.map((building) => {
                    const linked = manager.buildings.some((candidate) => candidate.id === building.id)
                    return (
                      <td key={building.id} className={`${ui.td} text-center`}>
                        <input
                          type="checkbox"
                          aria-label={`${manager.fullName} manages ${building.code}`}
                          checked={linked}
                          disabled={toggle.isPending}
                          onChange={() => toggle.mutate({ buildingId: building.id, userId: manager.userId, linked })}
                        />
                      </td>
                    )
                  })}
                </tr>
              ))}
              {managers.data.length === 0 && (
                <tr>
                  <td className={ui.td} colSpan={buildings.data.length + 1}>
                    There are no building manager accounts.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
