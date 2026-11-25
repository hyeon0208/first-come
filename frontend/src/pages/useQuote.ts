import type { Booking } from '../features/booking-shell'
import { isApiError } from '../shared/api/client'
import { api } from '../shared/api/endpoints'
import { useAsync } from '../shared/hooks/useAsync'
import { MESSAGES } from '../shared/messages'

// 선점한 좌석과 금액 요약. 시간이 끝났거나 좌석을 잃었으면 "다시 시도" 대신 그 안내를 띄운다.
export function useQuote(booking: Booking) {
  return useAsync(async () => {
    try {
      return await api.getQuote(booking.runId)
    } catch (error) {
      if (isApiError(error, 'TIME_OVER', 'SESSION_EXPIRED')) booking.reportError(error, MESSAGES.generalError)
      else if (isApiError(error, 'HOLD_NOT_FOUND')) booking.showSeatLost()
      throw error
    }
  }, [booking.runId])
}
