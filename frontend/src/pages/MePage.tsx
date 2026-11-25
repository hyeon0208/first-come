import { Button } from '@astryxdesign/core/Button'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { Skeleton } from '@astryxdesign/core/Skeleton'
import { VStack } from '@astryxdesign/core/VStack'
import { useNavigate } from 'react-router'
import { InProgressBanner, resumePath } from '../features/practice-start'
import { RecordTable } from '../features/records'
import { api } from '../shared/api/endpoints'
import { useAsync } from '../shared/hooks/useAsync'
import { MESSAGES } from '../shared/messages'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { PageFrame } from './PageFrame'

// 내 예매: 끝난 연습의 결과 목록.
export function MePage() {
  const navigate = useNavigate()
  const records = useAsync(api.getRecords, [])
  const current = useAsync(api.getCurrentPerformance, [])
  const performance = current.status === 'ready' ? current.data : null
  const startNew = <Button label={MESSAGES.buttons.newPractice} variant="primary" onClick={() => navigate('/')} />

  return (
    <PageFrame contentWidth={960}>
      <VStack gap={4}>
        {performance && <InProgressBanner onResume={() => navigate(resumePath(performance))} />}
        <HStack justify="between" align="center">
          <Heading level={1}>{MESSAGES.buttons.myBookings}</Heading>
          {records.status === 'ready' && records.data.length > 0 && startNew}
        </HStack>
        {records.status === 'loading' && (
          <VStack gap={2}>
            {[0, 1, 2, 3, 4].map((index) => (
              <Skeleton key={index} height={40} index={index} />
            ))}
          </VStack>
        )}
        {records.status === 'error' && <LoadFailed message={MESSAGES.records.failed} onRetry={records.reload} />}
        {records.status === 'ready' && records.data.length === 0 && <EmptyState title={MESSAGES.records.empty} actions={startNew} />}
        {records.status === 'ready' && records.data.length > 0 && <RecordTable records={records.data} />}
      </VStack>
    </PageFrame>
  )
}
