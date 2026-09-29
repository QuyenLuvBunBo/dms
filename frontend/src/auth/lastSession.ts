import type { Me } from '../api/types'

/**
 * Who this browser tab last had signed in: a user id, 'signed-out' after the Sign out button, or
 * null while nobody has signed in on this tab yet. Kept in sessionStorage, so neither a reload nor
 * the Back button after Sign out can bring the previous user's page back.
 */
export type LastSession = number | 'signed-out' | null

/** What RequireAuth hands to the login page: the page asked for, and who had the tab at that moment. */
export interface LoginRedirect {
  from: string
  lastSession: LastSession
}

const KEY = 'dms.lastSession'

/** Used when sessionStorage is unavailable (e.g. blocked by the browser). */
let inMemory: string | null = null

function write(value: string): void {
  inMemory = value
  try {
    sessionStorage.setItem(KEY, value)
  } catch {
    // sessionStorage unavailable: the in-memory value still covers this page load.
  }
}

export function readLastSession(): LastSession {
  let raw = inMemory
  try {
    raw = sessionStorage.getItem(KEY) ?? inMemory
  } catch {
    // sessionStorage unavailable: use the in-memory value.
  }
  if (raw === null) {
    return null
  }
  const userId = Number(raw)
  return raw !== 'signed-out' && Number.isInteger(userId) ? userId : 'signed-out'
}

export function rememberSignedIn(userId: number): void {
  write(String(userId))
}

export function rememberSignedOut(): void {
  write('signed-out')
}

/**
 * Where to go after signing in. The page RequireAuth remembered is used only when it can be this
 * user's own: nobody had signed in on this tab before (a link opened while signed out), or this same
 * user's session expired. After Sign out, or when someone else's session ended, it is the home page.
 */
export function landingPage(redirect: LoginRedirect | null | undefined, user: Me): string {
  if (!redirect?.from) {
    return '/'
  }
  const ownPage = redirect.lastSession === null || redirect.lastSession === user.id
  return ownPage ? redirect.from : '/'
}
