import { Button } from '@astryxdesign/core/Button'
import { Card } from '@astryxdesign/core/Card'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { useState } from 'react'
import type { Seat, SeatGrade } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatWon } from '../../shared/time/format'
import { groupByGrade, isQuantityComplete, ticketAmount } from './priceQuantity'

// 가격 선택: 등급별로 가격 종류("일반")의 수량을 좌석 수만큼 채운다.
export function PricePanel({
  seats,
  onBack,
  onSubmit,
}: {
  seats: Seat[]
  onBack: () => void
  onSubmit: (quantities: { grade: SeatGrade; quantity: number }[]) => Promise<void>
}) {
  const groups = groupByGrade(seats)
  const [quantities, setQuantities] = useState<Partial<Record<SeatGrade, number>>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isComplete = isQuantityComplete(groups, quantities)
  const total = groups.reduce((sum, group) => sum + (quantities[group.grade] ?? 0), 0)

  async function submit() {
    setIsSubmitting(true)
    try {
      await onSubmit(groups.map((group) => ({ grade: group.grade, quantity: quantities[group.grade] ?? 0 })))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <VStack gap={4} height="100%">
      <HStack gap={1} align="center">
        <Button label="좌석 선택으로 돌아가기" isIconOnly variant="ghost" icon={<BackIcon />} onClick={onBack} />
        <Heading level={2}>가격 선택</Heading>
      </HStack>
      <VStack gap={3}>
        {groups.map((group) => (
          <Card key={group.grade} padding={3}>
            <VStack gap={2}>
              <HStack justify="between" align="center">
                <Text weight="semibold">{group.gradeName}</Text>
                <Text hasTabularNumbers color="secondary">{`${quantities[group.grade] ?? 0}/${group.seats.length}`}</Text>
              </HStack>
              {group.seats.map((seat) => (
                <Text key={seat.seatId} color="secondary">
                  {seat.name}
                </Text>
              ))}
              <HStack justify="between" align="center" gap={2}>
                <Text>{`일반 ${formatWon(group.price)}`}</Text>
                <NumberInput
                  label={`${group.gradeName} 일반 수량`}
                  isLabelHidden
                  hasNumberSteppers
                  value={quantities[group.grade] ?? 0}
                  min={0}
                  max={group.seats.length}
                  width={120}
                  onChange={(value) => setQuantities((current) => ({ ...current, [group.grade]: Math.min(group.seats.length, Math.max(0, value)) }))}
                />
              </HStack>
            </VStack>
          </Card>
        ))}
      </VStack>
      <VStack gap={2}>
        <HStack justify="between">
          <Text>티켓 금액</Text>
          <Text weight="bold" hasTabularNumbers>
            {formatWon(ticketAmount(groups, quantities))}
          </Text>
        </HStack>
        <Button
          label={isComplete ? MESSAGES.buttons.bookCount(total) : MESSAGES.buttons.book}
          variant="primary"
          size="lg"
          isDisabled={!isComplete}
          isLoading={isSubmitting}
          onClick={submit}
        />
      </VStack>
    </VStack>
  )
}

function BackIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M10 3L5 8l5 5" />
    </svg>
  )
}
