import { useCallback, useEffect, useRef, useState } from 'react'
import { isApiError } from '../../shared/api/client'
import { api } from '../../shared/api/endpoints'
import type { BookingState, Seat, SeatStatuses } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'

// 한 회차에서 한 참가자가 가져갈 수 있는 매수
export const MAX_SEATS = 4

interface Options {
  runId: number
  mySeatIds: number[]
  // 화면 시계 기준 가선점이 끝나는 시각(밀리초). 고른 좌석이 없으면 null
  preselectEndsAt: number | null
  applyState: (state: BookingState) => void
  notify: (message: string, type?: 'info' | 'error') => void
  reportError: (error: unknown, fallbackMessage: string) => void
  onSoldOut: () => void
}

// 좌석 단계의 동작: 좌석을 누르면 서버에 가선점을 요청하고, 거절되면 그 좌석을 선택 불가로 바꾼다.
// 좌석 상태는 들어올 때 한 번 받고, 그 뒤로는 새로고침을 눌러야 다시 받는다(자동으로 다시 받지 않는다).
export function useSeatSelection({ runId, mySeatIds, preselectEndsAt, applyState, notify, reportError, onSoldOut }: Options) {
  const [unavailableSeatIds, setUnavailableSeatIds] = useState<ReadonlySet<number>>(new Set())
  const [pendingSeatId, setPendingSeatId] = useState<number | null>(null)
  // 들어올 때 한 번 받으므로 처음에는 받는 중이다.
  const [isRefreshing, setIsRefreshing] = useState(true)
  const [hasLoaded, setHasLoaded] = useState(false)
  const [hasLoadFailed, setHasLoadFailed] = useState(false)
  // 요청을 보내는 중인지. 상태 갱신보다 먼저 확인해야 해서 ref로도 둔다.
  const busy = useRef(false)

  const applyStatuses = useCallback(
    (statuses: SeatStatuses) => {
      setUnavailableSeatIds(new Set(statuses.unavailableSeatIds))
      setHasLoaded(true)
      setHasLoadFailed(false)
      setIsRefreshing(false)
      // 매진이고 내 좌석이 없으면 서버가 회차를 매진으로 끝냈다.
      if (statuses.soldOut && statuses.mySeatIds.length === 0) onSoldOut()
    },
    [onSoldOut],
  )

  const failStatuses = useCallback(
    (error: unknown) => {
      setIsRefreshing(false)
      if (isApiError(error, 'TIME_OVER', 'SESSION_EXPIRED')) reportError(error, MESSAGES.seat.loadFailed)
      else if (hasLoaded) notify(MESSAGES.generalError, 'error')
      else setHasLoadFailed(true)
    },
    [hasLoaded, notify, reportError],
  )

  const refresh = useCallback(() => {
    setIsRefreshing(true)
    return api.getSeatStatuses(runId).then(applyStatuses, failStatuses)
  }, [runId, applyStatuses, failStatuses])

  // 들어올 때 한 번만 받는다.
  useEffect(() => {
    api.getSeatStatuses(runId).then(applyStatuses, failStatuses)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [runId])

  const pressSeat = useCallback(
    async (seat: Seat) => {
      if (busy.current) {
        notify(MESSAGES.busy)
        return
      }
      const isMine = mySeatIds.includes(seat.seatId)
      if (!isMine) {
        if (unavailableSeatIds.has(seat.seatId)) return
        // 5번째 좌석은 서버에 보내지 않는다.
        if (mySeatIds.length >= MAX_SEATS) {
          notify(MESSAGES.seat.limit)
          return
        }
      }
      busy.current = true
      setPendingSeatId(seat.seatId)
      try {
        applyState(isMine ? await api.unpreselect(runId, seat.seatId) : await api.preselect(runId, seat.seatId))
      } catch (error) {
        if (isMine) {
          reportError(error, MESSAGES.seat.releaseFailed)
        } else if (isApiError(error, 'SEAT_TAKEN')) {
          // 화면에는 선택 가능으로 보였지만 이미 잡힌 좌석이다. 그 좌석만 바로 회색으로 바꾼다.
          setUnavailableSeatIds((current) => new Set(current).add(seat.seatId))
          notify(MESSAGES.seat.taken)
        } else if (isApiError(error, 'SOLD_OUT')) {
          setUnavailableSeatIds((current) => new Set(current).add(seat.seatId))
          onSoldOut()
        } else if (isApiError(error, 'SEAT_LIMIT_EXCEEDED')) {
          notify(MESSAGES.seat.limit)
        } else {
          reportError(error, MESSAGES.seat.selectFailed)
        }
      } finally {
        busy.current = false
        setPendingSeatId(null)
      }
    },
    [runId, mySeatIds, unavailableSeatIds, applyState, notify, reportError, onSoldOut],
  )

  const clearAll = useCallback(async () => {
    if (busy.current) {
      notify(MESSAGES.busy)
      return
    }
    busy.current = true
    try {
      applyState(await api.unpreselectAll(runId))
    } catch (error) {
      reportError(error, MESSAGES.seat.releaseFailed)
    } finally {
      busy.current = false
    }
  }, [runId, applyState, notify, reportError])

  // 가선점 시간이 끝나면 고른 좌석이 모두 풀린다. 그 좌석들만 선택 가능으로 되돌리고, 전체 좌석 상태는 다시 받지 않는다.
  useEffect(() => {
    if (preselectEndsAt === null || mySeatIds.length === 0) return
    const timer = window.setTimeout(
      async () => {
        notify(MESSAGES.seat.released)
        try {
          applyState(await api.getBookingState(runId))
        } catch (error) {
          reportError(error, MESSAGES.generalError)
        }
      },
      Math.max(0, preselectEndsAt - Date.now()) + 300,
    )
    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preselectEndsAt, mySeatIds.length > 0, runId])

  return { unavailableSeatIds, pendingSeatId, isRefreshing, hasLoaded, hasLoadFailed, refresh, pressSeat, clearAll }
}
