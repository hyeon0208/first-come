import { useEffect, useState, type ReactNode } from 'react'
import { api } from '../../shared/api/endpoints'
import { SessionContext, type Session } from './sessionContext'

// 닉네임을 넣은 사용자인지 처음에 한 번 확인한다(세션 쿠키).
export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session>({ status: 'loading' })

  useEffect(() => {
    api.getMe().then(
      (user) => setSession({ status: 'ready', user }),
      () => setSession({ status: 'ready', user: null }),
    )
  }, [])

  return (
    <SessionContext.Provider value={{ session, setUser: (user) => setSession({ status: 'ready', user }) }}>{children}</SessionContext.Provider>
  )
}
