import { describe, expect, it } from 'vitest'
import { formatDateTime, formatElapsed, formatRemaining, formatWon } from './format'
import { toClientTime } from './serverTime'

describe('시간과 금액 표시', () => {
  it('남은 시간은 분:초이고 0 아래로 내려가지 않는다', () => {
    expect(formatRemaining(588_000)).toBe('9:48')
    expect(formatRemaining(59_001)).toBe('1:00')
    expect(formatRemaining(-5_000)).toBe('0:00')
  })

  it('걸린 시간은 "1분 12초"처럼 보여 준다', () => {
    expect(formatElapsed(72)).toBe('1분 12초')
    expect(formatElapsed(9)).toBe('9초')
  })

  it('금액과 날짜', () => {
    expect(formatWon(132_000)).toBe('132,000원')
    expect(formatDateTime('2026-12-25T18:00:00')).toBe('2026.12.25(금) 18:00')
  })

  it('서버 시각은 서버의 "지금"과의 차이만큼 화면 시계에 더한다(소수점 이하 여섯 자리도 읽는다)', () => {
    expect(toClientTime('2026-12-01T10:00:10.500000', '2026-12-01T10:00:00.250000', 1_000)).toBe(1_000 + 10_250)
  })
})
