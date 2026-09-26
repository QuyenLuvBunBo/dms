import type { ReactNode } from 'react'
import type { Role } from '../api/types'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { useAuth } from './useAuth'

/**
 * Hides a screen from roles that should not see it. This only shapes the UI;
 * the backend enforces every permission on its own.
 */
export function RequireRole({ roles, children }: { roles: readonly Role[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) {
    return <ForbiddenPage />
  }
  return <>{children}</>
}
