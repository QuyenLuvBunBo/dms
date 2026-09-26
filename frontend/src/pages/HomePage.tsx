import { ROLE_LABELS } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { navItemsFor } from '../layout/navigation'

export function HomePage() {
  const { user } = useAuth()
  if (!user) {
    return null
  }
  const upcoming = navItemsFor(user.role).filter((item) => item.to !== '/')

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Welcome, {user.fullName}</h1>
        <p className="mt-1 text-sm text-slate-500">
          Signed in as <span className="font-medium text-slate-700">{user.username}</span> ({ROLE_LABELS[user.role]},{' '}
          {user.role})
        </p>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Your screens</h2>
        <ul className="mt-3 space-y-2 text-sm">
          {upcoming.map((item) => (
            <li key={item.to} className="flex items-center justify-between">
              <span>{item.label}</span>
              <span className="text-xs text-slate-400">Phase {item.phase}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
