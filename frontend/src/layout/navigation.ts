import type { Role } from '../api/types'

export interface NavItem {
  label: string
  to: string
  roles: readonly Role[]
  /** Build phase that delivers the screen; entries of later phases render a placeholder. */
  phase: number
}

const ALL_ROLES: readonly Role[] = ['STUDENT', 'ADMIN', 'BUILDING_MANAGER', 'TECHNICIAN', 'ACCOUNTANT']

/** Menus of the Spec v2 process. Paths are unique per screen; one screen may serve several roles. */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Home', to: '/', roles: ALL_ROLES, phase: 0 },

  { label: 'My declaration', to: '/my-declaration', roles: ['STUDENT'], phase: 2 },
  { label: 'Registration', to: '/registration', roles: ['STUDENT'], phase: 2 },
  { label: 'My residence', to: '/my-residence', roles: ['STUDENT'], phase: 3 },
  { label: 'Invoices', to: '/my-invoices', roles: ['STUDENT'], phase: 4 },
  { label: 'Repair tickets', to: '/my-tickets', roles: ['STUDENT'], phase: 5 },

  { label: 'Facilities', to: '/facilities', roles: ['ADMIN'], phase: 1 },
  { label: 'Building managers', to: '/building-managers', roles: ['ADMIN'], phase: 1 },
  { label: 'Registration rounds', to: '/registration-rounds', roles: ['ADMIN'], phase: 2 },
  { label: 'Warning reviews', to: '/warning-reviews', roles: ['ADMIN'], phase: 5 },
  { label: 'Reports', to: '/reports', roles: ['ADMIN'], phase: 6 },
  { label: 'Settings', to: '/settings', roles: ['ADMIN'], phase: 6 },
  { label: 'Audit log', to: '/audit-log', roles: ['ADMIN'], phase: 6 },

  { label: 'My buildings', to: '/facilities', roles: ['BUILDING_MANAGER'], phase: 1 },
  { label: 'Check-in', to: '/check-in', roles: ['BUILDING_MANAGER'], phase: 3 },
  { label: 'Meter readings', to: '/meter-readings', roles: ['BUILDING_MANAGER'], phase: 4 },
  { label: 'Repair tickets', to: '/repair-tickets', roles: ['BUILDING_MANAGER'], phase: 5 },
  { label: 'Violations', to: '/violations', roles: ['BUILDING_MANAGER'], phase: 5 },

  { label: 'Announcements', to: '/announcements', roles: ['STUDENT', 'BUILDING_MANAGER'], phase: 5 },

  { label: 'My tickets', to: '/technician/tickets', roles: ['TECHNICIAN'], phase: 5 },

  { label: 'Invoices', to: '/invoices', roles: ['ACCOUNTANT'], phase: 4 },
  { label: 'Payments', to: '/payments', roles: ['ACCOUNTANT'], phase: 4 },
]

export function navItemsFor(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.includes(role))
}
