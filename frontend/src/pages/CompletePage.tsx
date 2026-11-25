import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { MetadataList, MetadataListItem } from '@astryxdesign/core/MetadataList'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../shared/api/client'
import { api } from '../shared/api/endpoints'
import { useAsync } from '../shared/hooks/useAsync'
import { DIFFICULTY_NAMES, MESSAGES, RESULT_NAMES } from '../shared/messages'
import { formatDateTime, formatElapsed, formatWon } from '../shared/time/format'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { ColumnSkeleton } from './BookingColumn'
import { PageFrame } from './PageFrame'

// 예매 완료. 회차 데이터가 지워진 뒤에 다시 열면 예매번호·좌석·결제금액 없이 결과만 보인다.
export function CompletePage() {
  const performanceId = Number(useParams().runId)
  const showId = useSearchParams()[0].get('showId')
  const navigate = useNavigate()
  const result = useAsync(async () => {
    const record = await api.getRecord(performanceId)
    const reservation = await api.getReservation(performanceId).catch((error) => {
      if (isApiError(error, 'RESERVATION_NOT_FOUND')) return null
      throw error
    })
    return { record, reservation }
  }, [performanceId])

  return (
    <PageFrame contentWidth={840}>
      {result.status === 'loading' && <ColumnSkeleton />}
      {result.status === 'error' && <LoadFailed message={MESSAGES.complete.failed} onRetry={result.reload} />}
      {result.status === 'ready' && (
        <VStack gap={5}>
          <VStack gap={1}>
            <Heading level={1}>{MESSAGES.complete.title}</Heading>
            <Text color="secondary">{MESSAGES.complete.description}</Text>
          </VStack>
          <VStack gap={0.5}>
            <Text color="secondary">걸린 시간</Text>
            <Heading level={2} type="display-2">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{`오픈 뒤 ${formatElapsed(result.data.record.elapsedSeconds)}`}</span>
            </Heading>
          </VStack>
          <MetadataList columns="single">
            {result.data.reservation && <MetadataListItem label="예매번호">{result.data.reservation.reservationNo}</MetadataListItem>}
            {result.data.reservation && <MetadataListItem label="매수">{`총 ${result.data.reservation.seats.length}매`}</MetadataListItem>}
            {result.data.reservation && (
              <MetadataListItem label="좌석">{result.data.reservation.seats.map((seat) => seat.name).join(', ')}</MetadataListItem>
            )}
            <MetadataListItem label="공연">{result.data.record.showTitle}</MetadataListItem>
            {result.data.reservation && <MetadataListItem label="결제금액">{formatWon(result.data.reservation.amount)}</MetadataListItem>}
            <MetadataListItem label="예매상태">
              {result.data.record.result === 'SUCCESS' ? MESSAGES.complete.paid : RESULT_NAMES[result.data.record.result]}
            </MetadataListItem>
            <MetadataListItem label="난이도">{DIFFICULTY_NAMES[result.data.record.difficulty]}</MetadataListItem>
            <MetadataListItem label="끝난 시각">{formatDateTime(result.data.record.endedAt)}</MetadataListItem>
          </MetadataList>
          <HStack gap={2}>
            <Button label={MESSAGES.buttons.history} variant="primary" onClick={() => navigate('/me')} />
            <Button label={MESSAGES.buttons.restart} onClick={() => navigate(showId ? `/shows/${showId}` : '/')} />
          </HStack>
        </VStack>
      )}
    </PageFrame>
  )
}
