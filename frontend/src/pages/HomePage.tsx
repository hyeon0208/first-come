import { Center } from '@astryxdesign/core/Center'
import { Heading } from '@astryxdesign/core/Heading'
import { Spinner } from '@astryxdesign/core/Spinner'
import { VStack } from '@astryxdesign/core/VStack'
import { useNavigate } from 'react-router'
import { NicknameForm } from '../features/nickname'
import { InProgressBanner, resumePath } from '../features/practice-start'
import { useSession } from '../features/session'
import { ShowList } from '../features/shows'
import { api } from '../shared/api/endpoints'
import { useAsync } from '../shared/hooks/useAsync'
import { PageFrame } from './PageFrame'
import { expectedShowDate, SHOW_START_TIME } from './usePerformanceDate'

// 닉네임이 없으면 닉네임 입력, 있으면 공연 목록.
export function HomePage() {
  const { session, setUser } = useSession()

  if (session.status === 'loading') {
    return (
      <Center height="100dvh">
        <Spinner />
      </Center>
    )
  }
  if (session.user === null) {
    return (
      <Center minHeight="100dvh" padding={4}>
        <NicknameForm onCreated={setUser} />
      </Center>
    )
  }
  return <ShowListPage />
}

function ShowListPage() {
  const navigate = useNavigate()
  const current = useAsync(api.getCurrentPerformance, [])
  const performance = current.status === 'ready' ? current.data : null

  async function restart() {
    await api.abandonCurrentPerformance()
    current.reload()
  }

  return (
    <PageFrame>
      <VStack gap={4}>
        {performance && <InProgressBanner onResume={() => navigate(resumePath(performance))} onRestart={restart} />}
        <Heading level={1}>공연</Heading>
        <ShowList showDate={`${expectedShowDate().replaceAll('-', '.')} ${SHOW_START_TIME}`} onSelect={(showId) => navigate(`/shows/${showId}`)} />
      </VStack>
    </PageFrame>
  )
}
