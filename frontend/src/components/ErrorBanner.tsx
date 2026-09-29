import { ApiError } from '../api/client'

/** Shows why a request failed: the problem detail and, for 400s, the field errors. */
export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) {
    return null
  }
  const apiError = error instanceof ApiError ? error : null
  const message = apiError ? apiError.detail || apiError.title || apiError.message : 'Could not reach the server'
  return (
    <div role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
      <p>{message}</p>
      {apiError && apiError.errors.length > 0 && (
        <ul className="mt-1 list-disc pl-5">
          {apiError.errors.map((fieldError) => (
            <li key={`${fieldError.field}-${fieldError.message}`}>
              {fieldError.field}: {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
