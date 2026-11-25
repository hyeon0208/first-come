import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithTheme } from '../../test/renderWithTheme'
import { difficultyDescription } from './difficultyDescription'
import { InProgressBanner } from './InProgressBanner'
import { OpenCountdownButton } from './OpenCountdownButton'
import { resumePath } from './resumePath'

describe('OpenCountdownButton', () => {
  beforeEach(() => vi.useFakeTimers({ now: 1_000_000 }))
  afterEach(() => vi.useRealTimers())

  it('연습을 시작하기 전에는 "연습 시작"이고 누르면 onStart를 부른다', () => {
    const onStart = vi.fn()
    renderWithTheme(<OpenCountdownButton openAt={null} isCreating={false} isDisabled={false} onStart={onStart} onBook={vi.fn()} />)
    screen.getByRole('button', { name: '연습 시작' }).click()
    expect(onStart).toHaveBeenCalledOnce()
  })

  it('오픈 전에는 남은 시간을 보여 주며 꺼져 있고, 오픈 시각이 되면 "예매하기"로 바뀐다', () => {
    const onBook = vi.fn()
    renderWithTheme(<OpenCountdownButton openAt={1_000_000 + 7_000} isCreating={false} isDisabled={false} onStart={vi.fn()} onBook={onBook} />)
    const countdown = screen.getByRole('button', { name: '예매 오픈까지 0:07' })
    expect(countdown).toHaveAttribute('aria-disabled', 'true')

    act(() => vi.advanceTimersByTime(7_000))

    screen.getByRole('button', { name: '예매하기' }).click()
    expect(onBook).toHaveBeenCalledOnce()
  })
})

describe('진행 중 연습 띠', () => {
  it('안내 문구와 이어서 하기, 처음부터 다시를 보여 준다', async () => {
    const onResume = vi.fn()
    const onRestart = vi.fn()
    renderWithTheme(<InProgressBanner onResume={onResume} onRestart={onRestart} />)

    expect(screen.getByText('이미 진행 중인 예매가 있습니다.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '이어서 하기' }))
    await userEvent.click(screen.getByRole('button', { name: '처음부터 다시' }))
    expect(onResume).toHaveBeenCalledOnce()
    expect(onRestart).toHaveBeenCalledOnce()
  })

  it('입장 전 회차는 공연 상세로, 입장한 회차는 예매 화면으로 이어진다', () => {
    const performance = { id: 7, showId: 2, difficulty: 'EASY' as const, openAt: '', startAt: '', seatSelectionExpiresAt: null, active: true, serverNow: '' }
    expect(resumePath({ ...performance, enteredAt: null })).toBe('/shows/2')
    expect(resumePath({ ...performance, enteredAt: '2026-12-01T10:00:10' })).toBe('/practice/7/seat')
  })
})

describe('난이도 설명', () => {
  it('가상 경쟁자 수와 매진까지의 시간을 한 줄로 보여 준다', () => {
    expect(difficultyDescription({ difficulty: 'NORMAL', competitorCount: 500, arrivalSeconds: 120, soldOutSeconds: 100 })).toBe(
      '가상 경쟁자 500명 · 약 1분 40초에 매진',
    )
    expect(difficultyDescription({ difficulty: 'HARD', competitorCount: 1000, arrivalSeconds: 25, soldOutSeconds: 10 })).toBe(
      '가상 경쟁자 1,000명 · 약 10초에 매진',
    )
    expect(difficultyDescription({ difficulty: 'EASY', competitorCount: 200, arrivalSeconds: 300, soldOutSeconds: null })).toBe(
      '가상 경쟁자 200명 · 매진되지 않음',
    )
  })
})
