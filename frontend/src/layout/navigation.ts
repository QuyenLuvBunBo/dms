import type { Role } from '../api/types'

export interface NavItem {
  label: string
  to: string
  roles: readonly Role[]
  /** Build phase that delivers the screen; entries of later phases render a placeholder. */
  phase: number
}

const ALL_ROLES: readonly Role[] = ['STUDENT', 'ADMIN', 'TECHNICIAN', 'ACCOUNTANT', 'AFFAIRS']

export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Home', to: '/', roles: ALL_ROLES, phase: 0 },

  { label: 'My application', to: '/my-application', roles: ['STUDENT'], phase: 2 },
  { label: 'My room & contract', to: '/my-contract', roles: ['STUDENT'], phase: 3 },
  { label: 'Invoices', to: '/my-invoices', roles: ['STUDENT'], phase: 4 },
  { label: 'Repair tickets', to: '/my-tickets', roles: ['STUDENT'], phase: 5 },

  { label: 'Facilities', to: '/facilities', roles: ['ADMIN'], phase: 1 },
  { label: 'Admission rounds', to: '/admission-rounds', roles: ['ADMIN'], phase: 2 },
  { label: 'Applications', to: '/applications', roles: ['ADMIN'], phase: 2 },
  { label: 'Bed assignment', to: '/bed-assignment', roles: ['ADMIN'], phase: 3 },
  { label: 'Contracts', to: '/contracts', roles: ['ADMIN'], phase: 3 },
  { label: 'Meter readings', to: '/meter-readings', roles: ['ADMIN'], phase: 4 },
  { label: 'Repair tickets', to: '/repair-tickets', roles: ['ADMIN'], phase: 5 },
  { label: 'Violations', to: '/violations', roles: ['ADMIN', 'AFFAIRS'], phase: 5 },
  { label: 'Settings', to: '/settings', roles: ['ADMIN'], phase: 6 },
  { label: 'Audit log', to: '/audit-log', roles: ['ADMIN'], phase: 6 },

  { label: 'Invoices', to: '/invoices', roles: ['ACCOUNTANT'], phase: 4 },
  { label: 'Payments', to: '/payments', roles: ['ACCOUNTANT'], phase: 4 },

  { label: 'My tickets', to: '/technician/tickets', roles: ['TECHNICIAN'], phase: 5 },

  { label: 'Eviction proposals', to: '/eviction-proposals', roles: ['AFFAIRS'], phase: 5 },
]

export function navItemsFor(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.includes(role))
}
