import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { landingPage, type LoginRedirect } from '../auth/lastSession'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { user, isLoading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Never the previous user's page: see landingPage.
  const redirect = location.state as LoginRedirect | null

  if (!isLoading && user) {
    return <Navigate to={landingPage(redirect, user)} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const me = await login({ username, password })
      navigate(landingPage(redirect, me), { replace: true })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.detail || caught.title : 'Could not reach the server')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm space-y-5 rounded-lg border border-slate-200 bg-white p-8 shadow-sm"
      >
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Sign in to DMS</h1>
          <p className="mt-1 text-sm text-slate-500">Dormitory Management System</p>
        </div>

        <label className="block text-sm">
          <span className="font-medium text-slate-700">Username</span>
          <input
            name="username"
            autoComplete="username"
            autoFocus
            required
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 focus:border-slate-900 focus:outline-none"
          />
        </label>

        <label className="block text-sm">
          <span className="font-medium text-slate-700">Password</span>
          <input
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 focus:border-slate-900 focus:outline-none"
          />
        </label>

        {error && (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-60"
        >
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
