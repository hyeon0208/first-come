// 서버 응답의 모양. 시각은 시간대 표기가 없는 서버 시각 문자열이다(shared/time으로 다룬다).

export type SeatGrade = 'R' | 'S'
export type Difficulty = 'EASY' | 'NORMAL' | 'HARD'
export type PerformanceResult = 'SUCCESS' | 'SOLD_OUT' | 'TIMEOUT' | 'ABANDONED'
export type BookingPhase = 'SELECTING' | 'HELD' | 'PAYING' | 'RESERVED'

export interface User {
  id: number
  nickname: string
}

export interface SeatPrice {
  grade: SeatGrade
  name: string
  price: number
}

export interface Show {
  id: number
  title: string
  venueName: string
  runningMinutes: number
  ageLimit: string
  seatPrices: SeatPrice[]
}

export interface Seat {
  seatId: number
  floor: number
  block: string
  rowNo: number
  seatNo: number
  name: string
  grade: SeatGrade
  gradeName: string
  price: number
}

export interface SeatMapData {
  showId: number
  venueName: string
  seats: Seat[]
}

export interface DifficultyInfo {
  difficulty: Difficulty
  competitorCount: number
  arrivalSeconds: number
  soldOutSeconds: number | null
}

export interface Performance {
  id: number
  showId: number
  difficulty: Difficulty
  openAt: string
  startAt: string
  enteredAt: string | null
  seatSelectionExpiresAt: string | null
  active: boolean
  serverNow: string
}

export interface BookingState {
  performanceId: number
  showId: number
  phase: BookingPhase
  mySeatIds: number[]
  seatSelectionExpiresAt: string | null
  preselectExpiresAt: string | null
  holdExpiresAt: string | null
  serverNow: string
}

export interface SeatStatuses {
  unavailableSeatIds: number[]
  mySeatIds: number[]
  soldOut: boolean
}

export interface HoldResult {
  unselectableSeatIds: number[]
  state: BookingState
}

export interface ReservationSeat {
  seatId: number
  name: string
  gradeName: string
  price: number
}

export interface Quote {
  seats: ReservationSeat[]
  ticketAmount: number
  feeAmount: number
  amount: number
}

export interface Reservation {
  reservationId: number
  reservationNo: string
  performanceId: number
  status: 'PENDING' | 'CONFIRMED' | 'FAILED'
  seats: ReservationSeat[]
  ticketAmount: number
  feeAmount: number
  amount: number
  confirmedAt: string | null
}

// 표 컴포넌트에 넘기려면 인덱스로 읽을 수 있는 모양이어야 해서 type으로 둔다.
export type PracticeRecord = {
  id: number
  performanceId: number
  showTitle: string
  difficulty: Difficulty
  result: PerformanceResult
  elapsedSeconds: number
  endedAt: string
}
