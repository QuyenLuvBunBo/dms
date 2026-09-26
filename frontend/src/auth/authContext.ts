import { createContext } from 'react'
import type { LoginRequest } from '../api/auth'
import type { Me } from '../api/types'

export interface AuthContextValue {
  /** The signed-in user, or null when signed out. */
  user: Me | null
  /** True until the first GET /api/me has answered. */
  isLoading: boolean
  login: (request: LoginRequest) => Promise<Me>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
