import { Center } from '@astryxdesign/core/Center'
import { Dialog } from '@astryxdesign/core/Dialog'
import { Spinner } from '@astryxdesign/core/Spinner'
import { useRef } from 'react'
import { useNavigate } from 'react-router'
import { stepPath, useBooking } from '../features/booking-shell'
import { PaymentForm, payOutcome } from '../features/payment'
import { ApiError, isApiError } from '../shared/api/client'
import { api } from '../shared/api/endpoints'
import { useQuote } from './useQuote'
import { MESSAGES } from '../shared/messages'
import { BackHeading } from '../shared/ui/BackHeading'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { BookingColumn, ColumnSkeleton } from './BookingColumn'

async function isConfirmed(runId: number): Promise<boolean> {
  return api.getReservation(runId).then(
    (reservation) => reservation.status === 'CONFIRMED',
    () => false,
  )
}

// 결제. 결제하는 동안에는 화면 전체가 잠기고, 성공인지 시간 초과인지는 서버의 확정 단계가 정한다.
export function PayPage() {
  const booking = useBooking()
  const navigate = useNavigate()
  const quote = useQuote(booking)
  // 같은 요청을 다시 보낼 때는 같은 멱등 키를 쓴다. 서버가 답을 준 뒤에는(성공이든 거절이든) 새 키를 쓴다.
  const idempotencyKey = useRef<string | null>(null)

  async function pay() {
    idempotencyKey.current ??= crypto.randomUUID()
    booking.setPaying(true)
    try {
      await api.reserve(booking.runId, idempotencyKey.current)
      booking.complete()
    } catch (error) {
      // 응답을 받지 못했거나 거절로 보여도 서버에서는 확정됐을 수 있다(응답이 끊긴 뒤 다시 누른 경우 등).
      // 확정된 예매가 있으면 완료 화면으로 간다.
      if (await isConfirmed(booking.runId)) {
        booking.complete()
        return
      }
      // 서버가 답을 줬으면 새 키를 쓴다. 같은 키의 요청이 아직 처리 중이라는 답이면 그 키를 그대로 둔다.
      if (error instanceof ApiError && !isApiError(error, 'IDEMPOTENCY_KEY_IN_PROGRESS')) idempotencyKey.current = null
      const outcome = payOutcome(error)
      const isHoldOver = booking.clock.holdEndsAt !== null && Date.now() > booking.clock.holdEndsAt
      if (outcome === 'timeOverCancelled') booking.showTimeOver(true)
      else if (outcome === 'timeOver') booking.showTimeOver(false)
      else if (outcome === 'seatLost') booking.showSeatLost()
      else if (outcome === 'sessionExpired') booking.reportError(error, MESSAGES.generalError)
      // 결제하는 동안 예매 가능 시간이 끝났으면 화면에 머물게 두지 않고 종료를 알린다.
      else if (isHoldOver) booking.showTimeOver(false)
      else if (outcome === 'busy') booking.notify(MESSAGES.busy)
      else booking.notify(MESSAGES.pay.failed, 'error')
    } finally {
      booking.setPaying(false)
    }
  }

  return (
    <BookingColumn>
      <BackHeading
        title={MESSAGES.payment.title}
        backLabel="예매 정보 입력으로 돌아가기"
        isDisabled={booking.isPaying}
        onBack={() => navigate(stepPath(booking.runId, 'order'))}
      />
      {quote.status === 'loading' && <ColumnSkeleton />}
      {quote.status === 'error' && <LoadFailed message={MESSAGES.generalError} onRetry={quote.reload} />}
      {quote.status === 'ready' && <PaymentForm quote={quote.data} isPaying={booking.isPaying} onPay={pay} />}
      <Dialog isOpen={booking.isPaying} onOpenChange={() => undefined} purpose="required" padding={6}>
        <Center>
          <Spinner size="lg" label={MESSAGES.pay.processing} />
        </Center>
      </Dialog>
    </BookingColumn>
  )
}
