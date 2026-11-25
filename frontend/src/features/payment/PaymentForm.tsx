import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { MetadataList, MetadataListItem } from '@astryxdesign/core/MetadataList'
import { RadioList, RadioListItem } from '@astryxdesign/core/RadioList'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { useState } from 'react'
import type { Quote } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatWon } from '../../shared/time/format'

// 결제수단은 고르기만 하고 결과에 영향이 없다(모두 모의 결제).
const METHODS = [
  { value: 'card', label: '카드' },
  { value: 'easy', label: '간편결제' },
  { value: 'bank', label: '무통장' },
]

export function PaymentForm({ quote, isPaying, onPay }: { quote: Quote; isPaying: boolean; onPay: () => void }) {
  const [method, setMethod] = useState('')
  const [isMethodMissing, setIsMethodMissing] = useState(false)

  function pay() {
    if (method === '') {
      setIsMethodMissing(true)
      return
    }
    onPay()
  }

  return (
    <VStack gap={5}>
      <VStack gap={2}>
        <Heading level={2}>티켓 주문상세</Heading>
        <MetadataList columns="single">
          <MetadataListItem label="좌석정보">{quote.seats.map((seat) => seat.name).join(', ')}</MetadataListItem>
          <MetadataListItem label="가격정보">{quote.seats.map((seat) => `${seat.gradeName} ${formatWon(seat.price)}`).join(', ')}</MetadataListItem>
        </MetadataList>
      </VStack>
      <VStack gap={1}>
        <RadioList
          label={MESSAGES.payment.method}
          value={method}
          onChange={(value) => {
            setMethod(value)
            setIsMethodMissing(false)
          }}
        >
          {METHODS.map((item) => (
            <RadioListItem key={item.value} label={item.label} value={item.value} isDisabled={isPaying} />
          ))}
        </RadioList>
        {isMethodMissing && (
          <span role="alert" style={{ color: 'var(--color-text-error)' }}>
            <Text color="inherit">{MESSAGES.pay.selectMethod}</Text>
          </span>
        )}
      </VStack>
      <VStack gap={2}>
        <Heading level={2}>{MESSAGES.payment.info}</Heading>
        <MetadataList columns="single">
          <MetadataListItem label={MESSAGES.payment.ticketAmount}>{formatWon(quote.ticketAmount)}</MetadataListItem>
          <MetadataListItem label={MESSAGES.payment.fee}>{formatWon(quote.feeAmount)}</MetadataListItem>
          <MetadataListItem label={MESSAGES.payment.total}>{formatWon(quote.amount)}</MetadataListItem>
        </MetadataList>
      </VStack>
      <Button label={MESSAGES.buttons.pay(formatWon(quote.amount))} variant="primary" size="lg" isDisabled={isPaying} onClick={pay} />
    </VStack>
  )
}
