import { act, renderHook, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../shared/api/client'
import { api } from '../../shared/api/endpoints'
import type { BookingState, Seat } from '../../shared/api/types'
import { renderWithTheme } from '../../test/renderWithTheme'
import { SeatMap } from './SeatMap'
import { useSeatSelection } from './useSeatSelection'

function seat(seatId: number): Seat {
  return { seatId, floor: 1, block: 'B', rowNo: 3, seatNo: seatId, name: `1층 B구역 3열 ${seatId}번`, grade: 'R', gradeName: 'R석', price: 132_000 }
}

function state(mySeatIds: number[]): BookingState {
  return {
    performanceId: 1,
    showId: 1,
    phase: 'SELECTING',
    mySeatIds,
    seatSelectionExpiresAt: null,
    preselectExpiresAt: null,
    holdExpiresAt: null,
    serverNow: '2026-12-01T10:00:00',
  }
}

describe('SeatMap', () => {
  const seats = [seat(1), seat(2), seat(3)]
  const prices = [{ grade: 'R' as const, name: 'R석', price: 132_000 }]

  it('좌석마다 button 역할과 이름(위치, 등급, 상태)을 갖고, 누르면 그 좌석을 알린다', () => {
    const onSeatPress = vi.fn()
    renderWithTheme(
      <SeatMap
        seats={seats}
        prices={prices}
        unavailableSeatIds={new Set([2])}
        mySeatIds={new Set([3])}
        pendingSeatId={null}
        isInteractive
        isRefreshing={false}
        onSeatPress={onSeatPress}
        onRefresh={vi.fn()}
      />,
    )

    const unavailable = screen.getByRole('button', { name: '1층 B구역 3열 2번, R석, 선택 불가' })
    expect(unavailable).toHaveAttribute('aria-disabled', 'true')
    expect(unavailable).toHaveAttribute('tabindex', '-1')
    expect(screen.getByRole('button', { name: '1층 B구역 3열 3번, R석, 내 선택' })).toHaveAttribute('aria-pressed', 'true')
    screen.getByRole('button', { name: '1층 B구역 3열 1번, R석, 선택 가능' }).dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(onSeatPress).toHaveBeenCalledWith(seats[0])
  })

  it('범례에 등급 이름과 가격, 선택 불가를 보여 주고, 지도 버튼 넷이 있다', () => {
    renderWithTheme(
      <SeatMap
        seats={seats}
        prices={prices}
        unavailableSeatIds={new Set()}
        mySeatIds={new Set()}
        pendingSeatId={null}
        isInteractive
        isRefreshing={false}
        onSeatPress={vi.fn()}
        onRefresh={vi.fn()}
      />,
    )
    expect(screen.getByText('R석 132,000원')).toBeInTheDocument()
    expect(screen.getByText('선택 불가')).toBeInTheDocument()
    for (const name of ['좌석 상태 새로고침', '좌석도 확대', '좌석도 축소', '좌석도 전체보기']) {
      expect(screen.getByRole('button', { name })).toBeInTheDocument()
    }
  })
})

describe('좌석 단계의 동작', () => {
  const notify = vi.fn()
  const applyState = vi.fn()
  const reportError = vi.fn()
  const onSoldOut = vi.fn()

  beforeEach(() => {
    vi.restoreAllMocks()
    notify.mockReset()
    applyState.mockReset()
    onSoldOut.mockReset()
    vi.spyOn(api, 'getSeatStatuses').mockResolvedValue({ unavailableSeatIds: [9], mySeatIds: [], soldOut: false })
  })

  function render(mySeatIds: number[]) {
    return renderHook(() => useSeatSelection({ runId: 1, mySeatIds, preselectEndsAt: null, applyState, notify, reportError, onSoldOut }))
  }

  it('들어올 때 좌석 상태를 한 번 받는다', async () => {
    const { result } = render([])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))
    expect(api.getSeatStatuses).toHaveBeenCalledOnce()
    expect(result.current.unavailableSeatIds.has(9)).toBe(true)
  })

  it('좌석을 누르면 서버에 가선점을 요청하고 받은 상태를 반영한다', async () => {
    const preselect = vi.spyOn(api, 'preselect').mockResolvedValue(state([1]))
    const { result } = render([])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))

    await act(() => result.current.pressSeat(seat(1)))

    expect(preselect).toHaveBeenCalledWith(1, 1)
    expect(applyState).toHaveBeenCalledWith(state([1]))
  })

  it('5번째 좌석은 서버에 보내지 않고 매수 초과를 알린다', async () => {
    const preselect = vi.spyOn(api, 'preselect')
    const { result } = render([1, 2, 3, 4])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))

    await act(() => result.current.pressSeat(seat(5)))

    expect(preselect).not.toHaveBeenCalled()
    expect(notify).toHaveBeenCalledWith('선택 가능한 매수를 초과했어요.')
  })

  it('이미 잡힌 좌석이면 알리고, 그 좌석을 바로 선택 불가로 바꾼다', async () => {
    vi.spyOn(api, 'preselect').mockRejectedValue(new ApiError(409, 'SEAT_TAKEN', ''))
    const { result } = render([])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))

    await act(() => result.current.pressSeat(seat(1)))

    expect(notify).toHaveBeenCalledWith('선택하신 좌석이 이미 선점되었습니다. 다른 좌석을 선택해주세요.')
    expect(result.current.unavailableSeatIds.has(1)).toBe(true)
  })

  it('내 좌석을 다시 누르면 가선점을 푼다', async () => {
    const unpreselect = vi.spyOn(api, 'unpreselect').mockResolvedValue(state([]))
    const { result } = render([1])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))

    await act(() => result.current.pressSeat(seat(1)))

    expect(unpreselect).toHaveBeenCalledWith(1, 1)
  })

  it('요청을 처리하는 동안 다른 좌석을 누르면 보내지 않고 알린다', async () => {
    let finish: (value: BookingState) => void = () => undefined
    const preselect = vi.spyOn(api, 'preselect').mockReturnValue(new Promise((resolve) => (finish = resolve)))
    const { result } = render([])
    await waitFor(() => expect(result.current.hasLoaded).toBe(true))

    act(() => void result.current.pressSeat(seat(1)))
    await act(() => result.current.pressSeat(seat(2)))

    expect(preselect).toHaveBeenCalledOnce()
    expect(notify).toHaveBeenCalledWith('이전 요청을 처리 중이에요. 잠시 후 다시 시도해 주세요.')
    await act(async () => finish(state([1])))
  })

  it('매진이고 내 좌석이 없으면 매진을 알린다', async () => {
    vi.spyOn(api, 'getSeatStatuses').mockResolvedValue({ unavailableSeatIds: [1, 2], mySeatIds: [], soldOut: true })
    render([])
    await waitFor(() => expect(onSoldOut).toHaveBeenCalledOnce())
  })
})
