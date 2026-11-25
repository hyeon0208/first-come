import { describe, expect, it } from 'vitest'
import type { BookingState } from '../../shared/api/types'
import { activeTimer, stabilizeClock, timerNotice, toBookingClock } from './bookingClock'
import { leaveKind, stepOf, stepPath } from './steps'

const NOW = '2026-12-01T10:10:00'
function state(overrides: Partial<BookingState>): BookingState {
  return {
    performanceId: 1,
    showId: 1,
    phase: 'SELECTING',
    mySeatIds: [],
    seatSelectionExpiresAt: '2026-12-01T10:19:48',
    preselectExpiresAt: null,
    holdExpiresAt: null,
    serverNow: NOW,
    ...overrides,
  }
}

describe('머리에 보이는 타이머', () => {
  it('서버 시각과의 차이로 화면 시계의 끝나는 시각을 구한다(화면 시계가 어긋나 있어도 남은 시간이 맞다)', () => {
    const clock = toBookingClock(state({}), 5_000_000)
    expect(clock.seatSelectionEndsAt).toBe(5_000_000 + 588_000)
  })

  it('응답마다 조금씩 달라지는 끝나는 시각은 처음 값을 그대로 쓰고, 정말 바뀐 시각(새 가선점)만 바꾼다', () => {
    const first = { seatSelectionEndsAt: 600_000, preselectEndsAt: 420_000, holdEndsAt: null }
    const jittered = { seatSelectionEndsAt: 600_180, preselectEndsAt: 420_180, holdEndsAt: null }
    expect(stabilizeClock(first, jittered)).toEqual(first)

    const newWindow = { seatSelectionEndsAt: 600_180, preselectEndsAt: 500_000, holdEndsAt: 900_000 }
    expect(stabilizeClock(first, newWindow)).toEqual({ seatSelectionEndsAt: 600_000, preselectEndsAt: 500_000, holdEndsAt: 900_000 })
    expect(stabilizeClock(null, jittered)).toEqual(jittered)
  })

  it('좌석을 고르는 동안은 좌석 선택 시간(10분)을 보여 준다', () => {
    const current = state({})
    expect(activeTimer(current, toBookingClock(current, 0))).toMatchObject({ kind: 'seatSelection', label: '좌석 선택 시간', endsAt: 588_000 })
  })

  it('선점 중에는 좌석 선택 시간이 이미 끝났어도 예매 가능 시간(7분)을 보여 준다. 10분 종료 안내가 뜨지 않는다', () => {
    const current = state({ phase: 'HELD', seatSelectionExpiresAt: '2026-12-01T10:09:00', holdExpiresAt: '2026-12-01T10:16:00' })
    expect(activeTimer(current, toBookingClock(current, 0))).toMatchObject({ kind: 'booking', label: '예매 가능 시간', endsAt: 360_000 })
  })

  it('알림 문구는 단계마다 다르다', () => {
    expect(timerNotice('seatSelection', 180)).toBe('좌석 선택 가능 시간이 3분 남았어요')
    expect(timerNotice('seatSelection', 60)).toBe('좌석 선택 가능 시간이 1분 남았어요')
    expect(timerNotice('booking', 180)).toBe('결제 가능 시간이 3분 남았습니다.')
    expect(timerNotice('booking', 60)).toBe('1분 후, 결제 가능 시간이 종료됩니다.')
  })
})

describe('예매 화면의 단계와 뒤로 가기', () => {
  it('주소로 단계를 구분한다. 가격 선택은 좌석 화면의 ?step=price다', () => {
    expect(stepPath(3, 'price')).toBe('/practice/3/seat?step=price')
    expect(stepOf(3, '/practice/3/seat', '')).toBe('seat')
    expect(stepOf(3, '/practice/3/seat', '?step=price')).toBe('price')
    expect(stepOf(3, '/practice/3/pay', '')).toBe('pay')
    expect(stepOf(3, '/me', '')).toBeNull()
    expect(stepOf(3, '/practice/4/seat', '')).toBeNull()
  })

  it('예매 화면 밖으로 나가려 하면 예매를 종료할지 묻는다', () => {
    expect(leaveKind('seat', null, false)).toBe('exit')
    expect(leaveKind('pay', null, false)).toBe('exit')
  })

  it('가격 단계에서 좌석 단계로 돌아가려 하면 좌석이 풀린다고 묻고, 좌석 선택 시간이 끝났으면 막는다', () => {
    expect(leaveKind('price', 'seat', false)).toBe('backToSeat')
    expect(leaveKind('price', 'seat', true)).toBe('backBlocked')
  })

  it('예매 정보에서 가격으로는 묻고, 결제에서 예매 정보로는 묻지 않는다', () => {
    expect(leaveKind('order', 'price', false)).toBe('backToPrice')
    expect(leaveKind('pay', 'order', false)).toBe('allow')
  })
})
