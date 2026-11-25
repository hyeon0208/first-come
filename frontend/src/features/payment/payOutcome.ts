import { ApiError } from '../../shared/api/client'

// 결제 요청이 거절됐을 때 화면이 할 일.
export type PayOutcome =
  // 결제 뒤 확정 단계에서 시간이 끝났다. 결제는 자동으로 취소됐다
  | 'timeOverCancelled'
  // 결제를 부르기 전에 시간이 끝나 있었다
  | 'timeOver'
  // 같은 선점에 결제가 이미 진행 중이다(다른 탭 등)
  | 'busy'
  // 선점한 좌석이 없다
  | 'seatLost'
  | 'sessionExpired'
  // 결제가 거절됐거나 결과를 받지 못했다. 화면에 머문다
  | 'failed'

export function payOutcome(error: unknown): PayOutcome {
  if (!(error instanceof ApiError)) return 'failed'
  switch (error.code) {
    case 'BOOKING_TIME_EXPIRED':
      return 'timeOverCancelled'
    case 'TIME_OVER':
    case 'HOLD_EXPIRED':
    case 'PERFORMANCE_ALREADY_STARTED':
      return 'timeOver'
    case 'PAYMENT_IN_PROGRESS':
    case 'IDEMPOTENCY_KEY_IN_PROGRESS':
      return 'busy'
    case 'HOLD_NOT_FOUND':
      return 'seatLost'
    case 'SESSION_EXPIRED':
      return 'sessionExpired'
    default:
      return 'failed'
  }
}
