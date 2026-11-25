import { Button } from '@astryxdesign/core/Button'
import { useNow } from '../../shared/hooks/useNow'
import { MESSAGES } from '../../shared/messages'
import { openCountdownLabel } from './openCountdownLabel'

// 한 자리에서 차례로 바뀌는 버튼: 연습 시작 → 예매 오픈까지 0:07(꺼짐) → 예매하기.
// openAt은 화면 시계 기준 예매 오픈 시각(밀리초)이고, 연습을 시작하기 전에는 null이다.
export function OpenCountdownButton({
  openAt,
  isCreating,
  isDisabled,
  onStart,
  onBook,
}: {
  openAt: number | null
  isCreating: boolean
  isDisabled: boolean
  onStart: () => void
  onBook: () => void
}) {
  const now = useNow()

  if (openAt === null) {
    return <Button label={MESSAGES.buttons.startPractice} variant="primary" size="lg" isLoading={isCreating} isDisabled={isDisabled} onClick={onStart} />
  }
  if (now < openAt) {
    return <Button label={openCountdownLabel(openAt - now)} variant="primary" size="lg" isDisabled />
  }
  return <Button label={MESSAGES.buttons.book} variant="primary" size="lg" onClick={onBook} />
}
