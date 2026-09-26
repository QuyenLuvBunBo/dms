import { Link } from 'react-router'

export function ForbiddenPage() {
  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold tracking-tight">Access denied</h1>
      <p className="mt-2 text-sm text-slate-500">Your role does not have access to this screen.</p>
      <Link to="/" className="mt-4 inline-block text-sm font-medium text-slate-900 underline">
        Back to home
      </Link>
    </div>
  )
}
