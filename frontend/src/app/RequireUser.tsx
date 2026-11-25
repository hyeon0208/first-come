import { Center } from '@astryxdesign/core/Center'
import { Spinner } from '@astryxdesign/core/Spinner'
import { Navigate, Outlet } from 'react-router'
import { useSession } from '../features/session'

// 닉네임을 넣지 않았으면 처음 화면으로 보낸다.
export function RequireUser() {
  const { session } = useSession()
  if (session.status === 'loading') {
    return (
      <Center height="100dvh">
        <Spinner />
      </Center>
    )
  }
  return session.user === null ? <Navigate to="/" replace /> : <Outlet />
}
