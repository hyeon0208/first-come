import { Layout, LayoutContent, LayoutHeader } from '@astryxdesign/core/Layout'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { useSession } from '../features/session'
import { AppHeader } from '../shared/ui/AppHeader'

// 일반 화면의 틀: 머리와 가운데 내용.
export function PageFrame({ children, contentWidth = 1120 }: { children: ReactNode; contentWidth?: number }) {
  const navigate = useNavigate()
  const { session } = useSession()
  const nickname = session.status === 'ready' ? (session.user?.nickname ?? null) : null
  return (
    <Layout
      height="auto"
      contentWidth={contentWidth}
      header={
        <LayoutHeader>
          <AppHeader nickname={nickname} onHome={() => navigate('/')} onMyBookings={() => navigate('/me')} />
        </LayoutHeader>
      }
      content={<LayoutContent padding={5}>{children}</LayoutContent>}
    />
  )
}
