import { Button } from '@astryxdesign/core/Button'
import { CheckboxList, CheckboxListItem } from '@astryxdesign/core/CheckboxList'
import { Heading } from '@astryxdesign/core/Heading'
import { MetadataList, MetadataListItem } from '@astryxdesign/core/MetadataList'
import { RadioList, RadioListItem } from '@astryxdesign/core/RadioList'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { useState } from 'react'
import type { Quote } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatWon } from '../../shared/time/format'
import { hasAgreedAll, REQUIRED_TERMS } from './terms'

// 예매 정보 입력: 예매자, 수령 방법, 유의사항, 약관 동의. 개인 정보와 배송 칸은 없다.
export function OrderForm({ nickname, quote, onSubmit }: { nickname: string; quote: Quote; onSubmit: () => void }) {
  const [agreed, setAgreed] = useState<string[]>([])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isAllAgreed = hasAgreedAll(agreed)

  return (
    <VStack gap={5}>
      <VStack gap={2}>
        <Heading level={2}>{MESSAGES.order.booker}</Heading>
        <MetadataList columns="single">
          <MetadataListItem label="닉네임">{nickname}</MetadataListItem>
          <MetadataListItem label="좌석">{quote.seats.map((seat) => seat.name).join(', ')}</MetadataListItem>
          <MetadataListItem label={MESSAGES.payment.ticketAmount}>{formatWon(quote.ticketAmount)}</MetadataListItem>
        </MetadataList>
      </VStack>
      <RadioList label={MESSAGES.order.pickup} value="onSite" onChange={() => undefined}>
        <RadioListItem label={MESSAGES.order.pickupOnSite} value="onSite" />
      </RadioList>
      <VStack gap={2}>
        <Heading level={2}>{MESSAGES.order.notice}</Heading>
        <Text color="secondary">{MESSAGES.order.noticeBody}</Text>
      </VStack>
      <VStack gap={2}>
        <CheckboxList label={MESSAGES.order.terms} value={isAllAgreed ? ['all'] : []} onChange={(values) => setAgreed(values.includes('all') ? [...REQUIRED_TERMS] : [])}>
          <CheckboxListItem label={MESSAGES.order.agreeAll} value="all" />
        </CheckboxList>
        <CheckboxList label="필수 약관" isLabelHidden value={agreed} onChange={setAgreed}>
          <CheckboxListItem label={MESSAGES.order.cancelPolicy} value="cancelPolicy" />
          <CheckboxListItem label={MESSAGES.order.usagePolicy} value="usagePolicy" />
        </CheckboxList>
      </VStack>
      <Button
        label={MESSAGES.buttons.proceed}
        variant="primary"
        size="lg"
        isDisabled={!isAllAgreed}
        isLoading={isSubmitting}
        onClick={() => {
          setIsSubmitting(true)
          onSubmit()
        }}
      />
    </VStack>
  )
}
