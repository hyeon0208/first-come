import { act, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithTheme } from '../../test/renderWithTheme'
import { CountdownTimer } from './CountdownTimer'

describe('CountdownTimer', () => {
  beforeEach(() => vi.useFakeTimers({ now: 1_000_000 }))
  afterEach(() => vi.useRealTimers())

  it('끝나는 시각까지 남은 시간을 분:초로 보여 준다', () => {
    renderWithTheme(<CountdownTimer label="좌석 선택 시간" endsAt={1_000_000 + 588_000} />)
    expect(screen.getByText('좌석 선택 시간')).toBeInTheDocument()
    expect(screen.getByText('9:48')).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(60_000))

    expect(screen.getByText('8:48')).toBeInTheDocument()
  })

  it('3분과 1분이 남았을 때 한 번씩 알리고, 0이 되면 끝났다고 한 번 알린다', () => {
    const onNotify = vi.fn()
    const onExpire = vi.fn()
    renderWithTheme(
      <CountdownTimer label="좌석 선택 시간" endsAt={1_000_000 + 185_000} notifyAtSeconds={[180, 60]} onNotify={onNotify} onExpire={onExpire} />,
    )
    expect(onNotify).not.toHaveBeenCalled()

    act(() => vi.advanceTimersByTime(6_000))
    expect(onNotify.mock.calls).toEqual([[180]])

    act(() => vi.advanceTimersByTime(120_000))
    expect(onNotify.mock.calls).toEqual([[180], [60]])
    expect(onExpire).not.toHaveBeenCalled()

    act(() => vi.advanceTimersByTime(60_000))
    expect(onExpire).toHaveBeenCalledOnce()
    expect(screen.getByText('0:00')).toBeInTheDocument()
  })

  it('이미 3분 아래에서 시작한 타이머는 지난 3분 알림을 띄우지 않는다', () => {
    const onNotify = vi.fn()
    renderWithTheme(<CountdownTimer label="예매 가능 시간" endsAt={1_000_000 + 100_000} notifyAtSeconds={[180, 60]} onNotify={onNotify} />)
    act(() => vi.advanceTimersByTime(5_000))
    expect(onNotify).not.toHaveBeenCalled()
  })
})
