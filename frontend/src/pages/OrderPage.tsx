import { useNavigate } from 'react-router'
import { stepPath, useBooking } from '../features/booking-shell'
import { OrderForm } from '../features/order'
import { MESSAGES } from '../shared/messages'
import { BackHeading } from '../shared/ui/BackHeading'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { BookingColumn, ColumnSkeleton } from './BookingColumn'
import { useQuote } from './useQuote'

// 예매 정보 입력. 좌석과 금액 요약은 서버에서 다시 받는다.
export function OrderPage() {
  const booking = useBooking()
  const navigate = useNavigate()
  const quote = useQuote(booking)

  return (
    <BookingColumn>
      <BackHeading title={MESSAGES.order.title} backLabel="가격 선택으로 돌아가기" onBack={() => navigate(stepPath(booking.runId, 'price'))} />
      {quote.status === 'loading' && <ColumnSkeleton />}
      {quote.status === 'error' && <LoadFailed message={MESSAGES.generalError} onRetry={quote.reload} />}
      {quote.status === 'ready' && <OrderForm nickname={booking.nickname} quote={quote.data} onSubmit={() => booking.go('pay')} />}
    </BookingColumn>
  )
}
