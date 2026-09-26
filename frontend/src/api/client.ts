import { readCookie } from '../lib/cookies'
import type { FieldError, ProblemDetail } from './types'

const XSRF_COOKIE = 'XSRF-TOKEN'
const XSRF_HEADER = 'X-XSRF-TOKEN'

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail: string
  readonly errors: FieldError[]
  readonly rule: string | undefined

  constructor(status: number, problem: Partial<ProblemDetail>) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.title = problem.title ?? ''
    this.detail = problem.detail ?? ''
    this.errors = problem.errors ?? []
    this.rule = problem.rule
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestOptions {
  method?: Method
  body?: unknown
  /** Set by the "me" query, whose 401 simply means "signed out". */
  skipUnauthorizedHandler?: boolean
}

let onUnauthorized: (() => void) | undefined

/** Called when any request (other than the "me" query) comes back 401, i.e. the session ended. */
export function setUnauthorizedHandler(handler: (() => void) | undefined): void {
  onUnauthorized = handler
}

/**
 * The API writes the CSRF token to the XSRF-TOKEN cookie on every response, including the
 * 401 of an anonymous GET /api/me, so fetching the cookie never needs a dedicated endpoint.
 */
async function ensureCsrfToken(): Promise<string | undefined> {
  let token = readCookie(XSRF_COOKIE)
  if (!token) {
    await fetch('/api/me', { credentials: 'same-origin', headers: { Accept: 'application/json' } })
    token = readCookie(XSRF_COOKIE)
  }
  return token
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

/** Same-origin JSON request with session cookie, CSRF header on mutations and problem+json errors. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    const token = await ensureCsrfToken()
    if (token) {
      headers[XSRF_HEADER] = token
    }
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  const text = await response.text()
  const payload = text ? parseJson(text) : undefined

  if (!response.ok) {
    if (response.status === 401 && !options.skipUnauthorizedHandler) {
      onUnauthorized?.()
    }
    throw new ApiError(response.status, (payload as Partial<ProblemDetail> | undefined) ?? {})
  }
  return payload as T
}
