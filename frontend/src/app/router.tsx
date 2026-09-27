import { createBrowserRouter } from 'react-router'
import type { Role } from '../api/types'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { AppLayout } from '../layout/AppLayout'
import { NAV_ITEMS } from '../layout/navigation'
import { BuildingDetailPage } from '../pages/facilities/BuildingDetailPage'
import { BuildingListPage } from '../pages/facilities/BuildingListPage'
import { RoomDetailPage } from '../pages/facilities/RoomDetailPage'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { BuildingManagersPage } from '../pages/managers/BuildingManagersPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'

const FACILITY_ROLES: readonly Role[] = ['ADMIN', 'BUILDING_MANAGER']

// Menu entries of later phases get a role-guarded placeholder route, so the role-based
// navigation can be demonstrated end to end before their screens exist.
const placeholderRoutes = NAV_ITEMS.filter((item) => item.phase > 1).map((item) => ({
  path: item.to.slice(1),
  element: (
    <RequireRole roles={item.roles}>
      <PlaceholderPage title={item.label} phase={item.phase} />
    </RequireRole>
  ),
}))

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/',
        element: <AppLayout />,
        children: [
          { index: true, element: <HomePage /> },
          {
            path: 'facilities',
            element: (
              <RequireRole roles={FACILITY_ROLES}>
                <BuildingListPage />
              </RequireRole>
            ),
          },
          {
            path: 'facilities/buildings/:buildingId',
            element: (
              <RequireRole roles={FACILITY_ROLES}>
                <BuildingDetailPage />
              </RequireRole>
            ),
          },
          {
            path: 'facilities/rooms/:roomId',
            element: (
              <RequireRole roles={FACILITY_ROLES}>
                <RoomDetailPage />
              </RequireRole>
            ),
          },
          {
            path: 'building-managers',
            element: (
              <RequireRole roles={['ADMIN']}>
                <BuildingManagersPage />
              </RequireRole>
            ),
          },
          ...placeholderRoutes,
          { path: 'forbidden', element: <ForbiddenPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
