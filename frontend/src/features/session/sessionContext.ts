import { createContext, useContext } from 'react'
import type { User } from '../../shared/api/types'

export type Session = { status: 'loading' } | { status: 'ready'; user: User | null }

export const SessionContext = createContext<{ session: Session; setUser: (user: User) => void } | null>(null)

export function useSession() {
  const context = useContext(SessionContext)
  if (!context) throw new Error('SessionProvider 안에서만 쓸 수 있습니다.')
  return context
}
