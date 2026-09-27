import { useEffect, useMemo, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { ME_QUERY_KEY, fetchMe, login as loginRequest, logout as logoutRequest } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import type { Me } from '../api/types'
import { AuthContext, type AuthContextValue } from './authContext'
import { rememberSignedIn, rememberSignedOut } from './lastSession'

/**
 * Ends the session in this tab. Every cached API answer is dropped first, so the next user never
 * sees data that was fetched for the previous one (the facility lists are cached for 30 seconds).
 */
function endSession(queryClient: QueryClient): void {
  queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== ME_QUERY_KEY[0] })
  queryClient.setQueryData<Me | null>(ME_QUERY_KEY, null)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const meQuery = useQuery({ queryKey: ME_QUERY_KEY, queryFn: fetchMe })

  // The tab remembers who is signed in, so a later sign-in can tell whose page it would return to.
  useEffect(() => {
    if (meQuery.data) {
      rememberSignedIn(meQuery.data.id)
    }
  }, [meQuery.data])

  useEffect(() => {
    // A 401 means the server session expired: the same user may sign in again and continue.
    setUnauthorizedHandler(() => endSession(queryClient))
    return () => setUnauthorizedHandler(undefined)
  }, [queryClient])

  const loginMutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (me) => queryClient.setQueryData<Me | null>(ME_QUERY_KEY, me),
  })

  const logoutMutation = useMutation({
    mutationFn: logoutRequest,
    onSettled: () => {
      // Marked before the user is cleared, so the redirect to the login page already sees it.
      rememberSignedOut()
      endSession(queryClient)
    },
  })

  const { mutateAsync: login } = loginMutation
  const { mutateAsync: logout } = logoutMutation

  const value = useMemo<AuthContextValue>(
    () => ({
      user: meQuery.data ?? null,
      isLoading: meQuery.isPending,
      login,
      logout,
    }),
    [meQuery.data, meQuery.isPending, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
