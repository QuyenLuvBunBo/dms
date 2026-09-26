import { useEffect, useMemo, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ME_QUERY_KEY, fetchMe, login as loginRequest, logout as logoutRequest } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import type { Me } from '../api/types'
import { AuthContext, type AuthContextValue } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const meQuery = useQuery({ queryKey: ME_QUERY_KEY, queryFn: fetchMe })

  useEffect(() => {
    setUnauthorizedHandler(() => queryClient.setQueryData<Me | null>(ME_QUERY_KEY, null))
    return () => setUnauthorizedHandler(undefined)
  }, [queryClient])

  const loginMutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (me) => queryClient.setQueryData<Me | null>(ME_QUERY_KEY, me),
  })

  const logoutMutation = useMutation({
    mutationFn: logoutRequest,
    onSettled: () => queryClient.setQueryData<Me | null>(ME_QUERY_KEY, null),
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
