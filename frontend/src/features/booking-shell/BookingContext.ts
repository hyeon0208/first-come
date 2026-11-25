import { createContext, useContext } from 'react'
import type { BookingState, Seat, Show } from '../../shared/api/types'
import type { BookingClock } from './bookingClock'
import type { Step } from './steps'

// 예매 화면(좌석~결제)이 함께 쓰는 것.
export interface Booking {
  runId: number
  show: Show
  seats: Seat[]
  nickname: string
  state: BookingState
  clock: BookingClock
  step: Step
  isPaying: boolean
  setPaying: (isPaying: boolean) => void
  // 서버가 준 최신 상태를 반영한다
  applyState: (state: BookingState, receivedAt?: number) => void
  // 다음 단계로 간다(묻지 않는다)
  go: (step: Step) => void
  // 예매가 확정되어 완료 화면으로 간다
  complete: () => void
  // 시간 초과와 세션 만료는 대화상자로, 그 밖의 오류는 토스트로 알린다
  reportError: (error: unknown, fallbackMessage: string) => void
  notify: (message: string, type?: 'info' | 'error') => void
  showSoldOut: () => void
  showTimeOver: (isPaymentCancelled: boolean) => void
  showSeatLost: () => void
}

export const BookingContext = createContext<Booking | null>(null)

export function useBooking(): Booking {
  const booking = useContext(BookingContext)
  if (!booking) throw new Error('예매 화면 안에서만 쓸 수 있습니다.')
  return booking
}
