import { ApiError, apiFetch } from './client'
import type { Me } from './types'

export const ME_QUERY_KEY = ['me'] as const

export interface LoginRequest {
  username: string
  password: string
}

/** Resolves to null when there is no session; the request also seeds the CSRF cookie. */
export async function fetchMe(): Promise<Me | null> {
  try {
    return await apiFetch<Me>('/api/me', { skipUnauthorizedHandler: true })
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export function login(request: LoginRequest): Promise<Me> {
  return apiFetch<Me>('/api/auth/login', { method: 'POST', body: request })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}
