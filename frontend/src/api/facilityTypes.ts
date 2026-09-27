export type Gender = 'MALE' | 'FEMALE'

export type RoomAssetStatus = 'GOOD' | 'DAMAGED' | 'MISSING'

export const ROOM_ASSET_STATUSES: readonly RoomAssetStatus[] = ['GOOD', 'DAMAGED', 'MISSING']

export interface ManagerSummary {
  userId: number
  username: string
  fullName: string
}

export interface Building {
  id: number
  code: string
  name: string
  floorCount: number
  roomCount: number
  bedCount: number
  occupiedBedCount: number
  managers: ManagerSummary[]
}

export interface BuildingInput {
  code: string
  name: string
}

export interface Floor {
  id: number
  buildingId: number
  number: number
  /** Only the default filter of the room browser; never blocks a choice (BR-03). */
  genderPreference: Gender | null
  roomCount: number
}

export interface FloorInput {
  number: number
  genderPreference: Gender | null
}

export interface RoomType {
  id: number
  buildingId: number
  name: string
  capacity: number
  areaM2: number
  hasAirConditioning: boolean
  hasWaterHeater: boolean
  bathrooms: number
  /** VND per student per month. */
  monthlyRent: number
  roomCount: number
}

export type RoomTypeInput = Omit<RoomType, 'id' | 'buildingId' | 'roomCount'>

export interface RoomSummary {
  id: number
  code: string
  floorId: number
  floorNumber: number
  floorGenderPreference: Gender | null
  roomType: RoomType
  bedCount: number
  occupiedBedCount: number
  /** Null while the room is empty (BR-03). */
  gender: Gender | null
}

export interface Bed {
  id: number
  code: string
  occupied: boolean
}

export interface RoomAsset {
  id: number
  roomId: number
  name: string
  status: RoomAssetStatus
}

export interface RoomDetail {
  id: number
  code: string
  buildingId: number
  buildingCode: string
  floorId: number
  floorNumber: number
  floorGenderPreference: Gender | null
  roomType: RoomType
  gender: Gender | null
  beds: Bed[]
  assets: RoomAsset[]
}

export interface RoomInput {
  code: string
  roomTypeId: number
}

export interface AssetInput {
  name: string
  status: RoomAssetStatus
}

export interface ManagerAccount {
  userId: number
  username: string
  fullName: string
  buildings: { id: number; code: string }[]
}
