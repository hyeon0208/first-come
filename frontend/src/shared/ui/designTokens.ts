import type { CSSProperties } from 'react'

// 좌석 배치도와 가짜 포스터에 쓰는 색. 테마에 없는 화면 전용 값이라 여기 한 곳에만 둔다.
export const SEAT_TOKENS = {
  '--seat-grade-r': '#E8590C',
  '--seat-grade-s': '#0C8599',
  '--seat-unavailable': '#DDE1E6',
  '--seat-map-background': '#EEF0F3',
  '--seat-map-floor': '#FFFFFF',
  '--seat-stage': '#C9CED6',
} as CSSProperties

export const SEAT_GRADE_COLOR = { R: 'var(--seat-grade-r)', S: 'var(--seat-grade-s)' } as const
