import type { Gender, RoomAssetStatus } from '../api/facilityTypes'

const NUMBER = new Intl.NumberFormat('en-US')

/** Money is whole VND, e.g. 1,050,000 VND. */
export function formatVnd(amount: number): string {
  return `${NUMBER.format(amount)} VND`
}

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: 'Male',
  FEMALE: 'Female',
}

/** A room's gender: null means the room is empty and open to anyone (BR-03). */
export function roomGenderLabel(gender: Gender | null): string {
  return gender ? GENDER_LABELS[gender] : 'Empty'
}

export function floorPreferenceLabel(preference: Gender | null): string {
  return preference ? `${GENDER_LABELS[preference]} preferred` : 'No preference'
}

export const ASSET_STATUS_LABELS: Record<RoomAssetStatus, string> = {
  GOOD: 'Good',
  DAMAGED: 'Damaged',
  MISSING: 'Missing',
}

export function amenitiesLabel(type: {
  hasAirConditioning: boolean
  hasWaterHeater: boolean
  bathrooms: number
}): string {
  const parts = [
    type.hasAirConditioning ? 'Air conditioning' : 'No air conditioning',
    type.hasWaterHeater ? 'water heater' : 'no water heater',
    `${type.bathrooms} ${type.bathrooms === 1 ? 'bathroom' : 'bathrooms'}`,
  ]
  return parts.join(', ')
}
