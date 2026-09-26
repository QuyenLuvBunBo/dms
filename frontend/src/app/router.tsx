import { createBrowserRouter } from 'react-router'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { AppLayout } from '../layout/AppLayout'
import { NAV_ITEMS } from '../layout/navigation'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'

// Every menu entry of a later phase gets a role-guarded placeholder route now, so the
// role-based navigation can be demonstrated end to end before the screens exist.
const placeholderRoutes = NAV_ITEMS.filter((item) => item.to !== '/').map((item) => ({
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
          ...placeholderRoutes,
          { path: 'forbidden', element: <ForbiddenPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
