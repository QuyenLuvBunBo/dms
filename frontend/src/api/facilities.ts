import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from './client'
import type {
  AssetInput,
  Bed,
  Building,
  BuildingInput,
  Floor,
  FloorInput,
  ManagerAccount,
  RoomAsset,
  RoomDetail,
  RoomInput,
  RoomSummary,
  RoomType,
  RoomTypeInput,
} from './facilityTypes'

export const facilityKeys = {
  buildings: ['buildings'] as const,
  building: (id: number) => ['buildings', id] as const,
  floors: (buildingId: number) => ['buildings', buildingId, 'floors'] as const,
  roomTypes: (buildingId: number) => ['buildings', buildingId, 'room-types'] as const,
  rooms: (buildingId: number) => ['buildings', buildingId, 'rooms'] as const,
  room: (id: number) => ['rooms', id] as const,
  managers: ['building-managers'] as const,
}

export const listBuildings = () => apiFetch<Building[]>('/api/buildings')
export const getBuilding = (id: number) => apiFetch<Building>(`/api/buildings/${id}`)
export const createBuilding = (input: BuildingInput) =>
  apiFetch<Building>('/api/buildings', { method: 'POST', body: input })
export const updateBuilding = (id: number, input: BuildingInput) =>
  apiFetch<Building>(`/api/buildings/${id}`, { method: 'PUT', body: input })
export const deleteBuilding = (id: number) => apiFetch<void>(`/api/buildings/${id}`, { method: 'DELETE' })

export const listFloors = (buildingId: number) => apiFetch<Floor[]>(`/api/buildings/${buildingId}/floors`)
export const createFloor = (buildingId: number, input: FloorInput) =>
  apiFetch<Floor>(`/api/buildings/${buildingId}/floors`, { method: 'POST', body: input })
export const updateFloor = (id: number, input: FloorInput) =>
  apiFetch<Floor>(`/api/floors/${id}`, { method: 'PUT', body: input })
export const deleteFloor = (id: number) => apiFetch<void>(`/api/floors/${id}`, { method: 'DELETE' })

export const listRoomTypes = (buildingId: number) =>
  apiFetch<RoomType[]>(`/api/buildings/${buildingId}/room-types`)
export const createRoomType = (buildingId: number, input: RoomTypeInput) =>
  apiFetch<RoomType>(`/api/buildings/${buildingId}/room-types`, { method: 'POST', body: input })
export const updateRoomType = (id: number, input: RoomTypeInput) =>
  apiFetch<RoomType>(`/api/room-types/${id}`, { method: 'PUT', body: input })
export const deleteRoomType = (id: number) => apiFetch<void>(`/api/room-types/${id}`, { method: 'DELETE' })

export const listRooms = (buildingId: number) => apiFetch<RoomSummary[]>(`/api/buildings/${buildingId}/rooms`)
export const getRoom = (id: number) => apiFetch<RoomDetail>(`/api/rooms/${id}`)
export const createRoom = (floorId: number, input: RoomInput) =>
  apiFetch<RoomDetail>(`/api/floors/${floorId}/rooms`, { method: 'POST', body: input })
export const updateRoom = (id: number, input: RoomInput) =>
  apiFetch<RoomDetail>(`/api/rooms/${id}`, { method: 'PUT', body: input })
export const deleteRoom = (id: number) => apiFetch<void>(`/api/rooms/${id}`, { method: 'DELETE' })

/** Without a code the bed gets the lowest free number. */
export const addBed = (roomId: number, code?: string) =>
  apiFetch<Bed>(`/api/rooms/${roomId}/beds`, { method: 'POST', body: code ? { code } : {} })
export const relabelBed = (id: number, code: string) =>
  apiFetch<Bed>(`/api/beds/${id}`, { method: 'PUT', body: { code } })
export const removeBed = (id: number) => apiFetch<void>(`/api/beds/${id}`, { method: 'DELETE' })

export const createAsset = (roomId: number, input: AssetInput) =>
  apiFetch<RoomAsset>(`/api/rooms/${roomId}/assets`, { method: 'POST', body: input })
export const updateAsset = (id: number, input: AssetInput) =>
  apiFetch<RoomAsset>(`/api/room-assets/${id}`, { method: 'PUT', body: input })
export const deleteAsset = (id: number) => apiFetch<void>(`/api/room-assets/${id}`, { method: 'DELETE' })

export const listManagers = () => apiFetch<ManagerAccount[]>('/api/building-managers')
export const assignManager = (buildingId: number, userId: number) =>
  apiFetch<void>(`/api/buildings/${buildingId}/managers/${userId}`, { method: 'PUT' })
export const unassignManager = (buildingId: number, userId: number) =>
  apiFetch<void>(`/api/buildings/${buildingId}/managers/${userId}`, { method: 'DELETE' })

/**
 * A mutation that refreshes every facility screen afterwards: counts, room lists and manager
 * links all depend on each other, and the data set is small.
 */
export function useFacilityMutation<TVariables = void, TResult = unknown>(
  mutationFn: (variables: TVariables) => Promise<TResult>,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: facilityKeys.buildings }),
        queryClient.invalidateQueries({ queryKey: ['rooms'] }),
        queryClient.invalidateQueries({ queryKey: facilityKeys.managers }),
      ]),
  })
}
