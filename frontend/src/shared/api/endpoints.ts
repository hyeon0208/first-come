import { ApiError, request } from './client'
import type {
  BookingState,
  Difficulty,
  DifficultyInfo,
  HoldResult,
  Performance,
  PracticeRecord,
  Quote,
  Reservation,
  SeatGrade,
  SeatMapData,
  SeatStatuses,
  Show,
  User,
} from './types'

export const api = {
  createUser: (nickname: string) => request<User>('/users', { method: 'POST', body: { nickname } }),

  // 닉네임을 아직 넣지 않았으면 null
  getMe: async (): Promise<User | null> => {
    try {
      return await request<User>('/users/me')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null
      throw error
    }
  },

  getShows: () => request<Show[]>('/shows'),
  getShow: (showId: number) => request<Show>(`/shows/${showId}`),
  getSeatMap: (showId: number) => request<SeatMapData>(`/shows/${showId}/seat-map`),
  getDifficulties: () => request<DifficultyInfo[]>('/difficulties'),

  createPerformance: (showId: number, difficulty: Difficulty) =>
    request<Performance>('/performances', { method: 'POST', body: { showId, difficulty } }),
  // 진행 중인 회차가 없으면 null
  getCurrentPerformance: async (): Promise<Performance | null> => (await request<Performance | undefined>('/performances/current')) ?? null,
  abandonCurrentPerformance: () => request<void>('/performances/current/abandon', { method: 'POST' }),

  enter: (runId: number) => request<BookingState>('/booking/enter', { method: 'POST', runId }),
  getBookingState: (runId: number) => request<BookingState>('/booking/state', { runId }),
  getSeatStatuses: (runId: number) => request<SeatStatuses>('/booking/seats', { runId }),
  preselect: (runId: number, seatId: number) => request<BookingState>(`/booking/seats/${seatId}/preselect`, { method: 'POST', runId }),
  unpreselect: (runId: number, seatId: number) => request<BookingState>(`/booking/seats/${seatId}/preselect`, { method: 'DELETE', runId }),
  unpreselectAll: (runId: number) => request<BookingState>('/booking/preselects', { method: 'DELETE', runId }),
  hold: (runId: number) => request<HoldResult>('/booking/hold', { method: 'POST', runId }),
  releaseHold: (runId: number) => request<BookingState>('/booking/hold', { method: 'DELETE', runId }),
  validatePrices: (runId: number, quantities: { grade: SeatGrade; quantity: number }[]) =>
    request<void>('/booking/prices/validate', { method: 'POST', runId, body: { quantities } }),

  getQuote: (runId: number) => request<Quote>('/reservations/quote', { runId }),
  reserve: (runId: number, idempotencyKey: string) =>
    request<Reservation>('/reservations', { method: 'POST', runId, headers: { 'Idempotency-Key': idempotencyKey } }),
  getReservation: (performanceId: number) => request<Reservation>(`/reservations/by-performance/${performanceId}`),

  getRecords: () => request<PracticeRecord[]>('/records'),
  getRecord: (performanceId: number) => request<PracticeRecord>(`/records/by-performance/${performanceId}`),
}
