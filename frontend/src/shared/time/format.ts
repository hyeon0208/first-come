import { splitServerTime } from './serverTime'

// 남은 시간을 "9:48"처럼 보여 준다. 0 아래로 내려가지 않는다.
export function formatRemaining(milliseconds: number): string {
  const totalSeconds = Math.max(0, Math.ceil(milliseconds / 1000))
  return `${Math.floor(totalSeconds / 60)}:${String(totalSeconds % 60).padStart(2, '0')}`
}

// 걸린 시간을 "1분 12초"처럼 보여 준다.
export function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return minutes > 0 ? `${minutes}분 ${seconds}초` : `${seconds}초`
}

export function formatWon(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// "2026.12.25(금) 18:00"
export function formatDateTime(value: string): string {
  const { year, month, day, hour, minute } = splitServerTime(value)
  const weekday = WEEKDAYS[new Date(year, month - 1, day).getDay()]
  return `${year}.${pad(month)}.${pad(day)}(${weekday}) ${pad(hour)}:${pad(minute)}`
}

// 달력에 넘기는 "2026-12-25"
export function toIsoDate(value: string): string {
  return value.slice(0, 10)
}

export function formatTime(value: string): string {
  const { hour, minute } = splitServerTime(value)
  return `${pad(hour)}:${pad(minute)}`
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}
