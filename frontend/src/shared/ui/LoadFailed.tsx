import { Button } from '@astryxdesign/core/Button'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { MESSAGES } from '../messages'

// 불러오지 못했을 때: 문장과 "다시 시도".
export function LoadFailed({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <EmptyState title={message} actions={<Button label={MESSAGES.buttons.retry} onClick={onRetry} />} />
}
