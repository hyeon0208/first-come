import type { BookingState } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { toClientTimeOrNull } from '../../shared/time/serverTime'

// 서버가 준 세 시각을 화면 시계로 옮긴 값(밀리초).
export interface BookingClock {
  seatSelectionEndsAt: number | null
  preselectEndsAt: number | null
  holdEndsAt: number | null
}

export function toBookingClock(state: BookingState, receivedAt: number): BookingClock {
  return {
    seatSelectionEndsAt: toClientTimeOrNull(state.seatSelectionExpiresAt, state.serverNow, receivedAt),
    preselectEndsAt: toClientTimeOrNull(state.preselectExpiresAt, state.serverNow, receivedAt),
    holdEndsAt: toClientTimeOrNull(state.holdExpiresAt, state.serverNow, receivedAt),
  }
}

// 같은 서버 시각도 응답마다 받은 순간이 달라 화면 시계로 옮긴 값이 조금씩(응답이 걸린 만큼) 달라진다.
// 그 차이로 타이머가 다른 것으로 보이지 않도록, 거의 같은 값이면 먼저 구한 값을 그대로 쓴다.
const JITTER_MS = 1500

export function stabilizeClock(previous: BookingClock | null, next: BookingClock): BookingClock {
  if (previous === null) return next
  const keep = (before: number | null, after: number | null) =>
    before !== null && after !== null && Math.abs(before - after) < JITTER_MS ? before : after
  return {
    seatSelectionEndsAt: keep(previous.seatSelectionEndsAt, next.seatSelectionEndsAt),
    preselectEndsAt: keep(previous.preselectEndsAt, next.preselectEndsAt),
    holdEndsAt: keep(previous.holdEndsAt, next.holdEndsAt),
  }
}

export interface ActiveTimer {
  kind: 'seatSelection' | 'booking'
  label: string
  endsAt: number
  // 남은 시간이 이 초에 닿을 때 알림을 띄운다
  notifyAtSeconds: number[]
}

// 지금 머리에 보일 타이머.
// 좌석을 고르는 동안은 좌석 선택 시간(10분)이고, 선택 완료로 선점한 뒤에는 예매 가능 시간(7분)이다.
// 좌석 선택 시간이 끝나도 선점 중이면 예매 가능 시간을 끝까지 주므로, 선점한 뒤에는 좌석 선택 시간을 보지 않는다.
export function activeTimer(state: BookingState, clock: BookingClock): ActiveTimer | null {
  if (state.phase === 'SELECTING') {
    return clock.seatSelectionEndsAt === null
      ? null
      : { kind: 'seatSelection', label: MESSAGES.timer.seatSelection, endsAt: clock.seatSelectionEndsAt, notifyAtSeconds: [180, 60] }
  }
  return clock.holdEndsAt === null ? null : { kind: 'booking', label: MESSAGES.timer.booking, endsAt: clock.holdEndsAt, notifyAtSeconds: [180, 60] }
}

// 알림 문구. 좌석 단계는 "좌석 선택 가능 시간이 N분 남았어요", 그 뒤 단계는 3분과 마지막 1분의 문구가 다르다.
export function timerNotice(kind: ActiveTimer['kind'], secondsLeft: number): string {
  const minutes = Math.round(secondsLeft / 60)
  if (kind === 'seatSelection') return MESSAGES.seatTimeLeft(minutes)
  return minutes <= 1 ? MESSAGES.payTimeLastMinute : MESSAGES.payTimeLeft(minutes)
}
