import { Card } from '@astryxdesign/core/Card'
import { HStack } from '@astryxdesign/core/HStack'
import { IconButton } from '@astryxdesign/core/IconButton'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import type { Seat } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatWon } from '../../shared/time/format'

// 오른쪽 칸의 고른 좌석 카드 묶음: 등급, 위치, 가격, 빼기(×).
export function SelectedSeatList({ seats, isDisabled, onRemove }: { seats: Seat[]; isDisabled: boolean; onRemove: (seat: Seat) => void }) {
  if (seats.length === 0) return <Text color="secondary">{MESSAGES.seat.none}</Text>
  return (
    <VStack gap={2}>
      {seats.map((seat) => (
        <Card key={seat.seatId} padding={3}>
          <HStack justify="between" align="center" gap={2}>
            <VStack gap={0.5}>
              <Text weight="semibold">{seat.gradeName}</Text>
              <Text color="secondary">{seat.name}</Text>
              <Text>{formatWon(seat.price)}</Text>
            </VStack>
            <IconButton label={`${seat.name} 빼기`} variant="ghost" icon={<RemoveIcon />} isDisabled={isDisabled} onClick={() => onRemove(seat)} />
          </HStack>
        </Card>
      ))}
    </VStack>
  )
}

function RemoveIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true">
      <path d="M4 4l8 8M12 4l-8 8" />
    </svg>
  )
}
