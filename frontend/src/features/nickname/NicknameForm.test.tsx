import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithTheme } from '../../test/renderWithTheme'
import { NicknameForm } from './NicknameForm'
import { isValidNickname } from './nicknameRule'

describe('닉네임 규칙(앞뒤 공백을 빼고 2~12자)', () => {
  it('2자와 12자는 되고, 1자·13자·공백만은 안 된다', () => {
    expect(isValidNickname('연습')).toBe(true)
    expect(isValidNickname('가'.repeat(12))).toBe(true)
    expect(isValidNickname('가')).toBe(false)
    expect(isValidNickname('가'.repeat(13))).toBe(false)
    expect(isValidNickname('     ')).toBe(false)
  })
})

describe('NicknameForm', () => {
  it('비어 있으면 시작하기가 꺼져 있고, 1자를 넣으면 안내 문구가 보인다', async () => {
    renderWithTheme(<NicknameForm onCreated={vi.fn()} />)
    const start = screen.getByRole('button', { name: '시작하기' })
    expect(start).toHaveAttribute('aria-disabled', 'true')

    await userEvent.type(screen.getByLabelText('닉네임'), '가')

    expect(screen.getByText('닉네임은 2~12자로 입력해 주세요.')).toBeInTheDocument()
    expect(start).toHaveAttribute('aria-disabled', 'true')
  })
})
