import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold tracking-tight">Page not found</h1>
      <p className="mt-2 text-sm text-slate-500">There is nothing at this address.</p>
      <Link to="/" className="mt-4 inline-block text-sm font-medium text-slate-900 underline">
        Back to home
      </Link>
    </div>
  )
}
