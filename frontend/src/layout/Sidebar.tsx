import { NavLink } from 'react-router'
import { ROLE_LABELS } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { navItemsFor } from './navigation'

export function Sidebar() {
  const { user, logout } = useAuth()
  if (!user) {
    return null
  }
  const items = navItemsFor(user.role)

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-6 py-5">
        <div className="text-lg font-semibold tracking-tight">DMS</div>
        <div className="text-xs text-slate-500">Dormitory Management System</div>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-4" aria-label="Main">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `block rounded-md px-3 py-2 text-sm font-medium ${
                isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-slate-200 px-6 py-4">
        <div className="text-sm font-medium">{user.fullName}</div>
        <div className="text-xs text-slate-500">{ROLE_LABELS[user.role]}</div>
        <button
          type="button"
          onClick={() => {
            logout().catch(() => undefined)
          }}
          className="mt-3 w-full rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
        >
          Sign out
        </button>
      </div>
    </aside>
  )
}
