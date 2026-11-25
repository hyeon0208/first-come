import { describe, expect, it } from 'vitest'
import { ApiError, NetworkError } from '../../shared/api/client'
import { payOutcome } from './payOutcome'

describe('결제가 거절됐을 때 화면이 할 일', () => {
  it('결제 뒤 시간 초과는 "결제는 자동으로 취소되었어요"를 함께 알린다', () => {
    expect(payOutcome(new ApiError(409, 'BOOKING_TIME_EXPIRED', ''))).toBe('timeOverCancelled')
  })

  it('결제 중에 다시 요청되면 처리 중이라고 알리고 화면은 그대로 둔다', () => {
    expect(payOutcome(new ApiError(409, 'PAYMENT_IN_PROGRESS', ''))).toBe('busy')
    expect(payOutcome(new ApiError(409, 'IDEMPOTENCY_KEY_IN_PROGRESS', ''))).toBe('busy')
  })

  it('선점한 좌석이 없으면 좌석을 잃었다고 알린다', () => {
    expect(payOutcome(new ApiError(409, 'HOLD_NOT_FOUND', ''))).toBe('seatLost')
  })

  it('결제 거절과 응답 없음은 화면에 머무는 실패다', () => {
    expect(payOutcome(new ApiError(402, 'PAYMENT_DECLINED', ''))).toBe('failed')
    expect(payOutcome(new ApiError(502, 'PAYMENT_UNAVAILABLE', ''))).toBe('failed')
    expect(payOutcome(new NetworkError())).toBe('failed')
  })
})
