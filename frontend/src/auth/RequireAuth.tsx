import { Navigate, Outlet, useLocation } from 'react-router'
import { readLastSession, type LoginRedirect } from './lastSession'
import { useAuth } from './useAuth'

/**
 * Gate for everything behind the login page. Remembers where the user wanted to go, together with
 * who had this tab, so that the login page returns there only for that same user.
 */
export function RequireAuth() {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">Loading...</div>
    )
  }
  if (!user) {
    const redirect: LoginRedirect = { from: location.pathname + location.search, lastSession: readLastSession() }
    return <Navigate to="/login" replace state={redirect} />
  }
  return <Outlet />
}
