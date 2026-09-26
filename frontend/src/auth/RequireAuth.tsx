import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'

/** Gate for everything behind the login page. Remembers where the user wanted to go. */
export function RequireAuth() {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">Loading...</div>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
