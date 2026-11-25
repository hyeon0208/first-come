import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { HStack } from '@astryxdesign/core/HStack'
import { MESSAGES } from '../../shared/messages'

// 진행 중인 연습이 있을 때의 띠: 이어서 하기 / 처음부터 다시.
export function InProgressBanner({ onResume, onRestart }: { onResume: () => void; onRestart?: () => void }) {
  return (
    <Banner
      status="info"
      title={MESSAGES.inProgress}
      collapsible={false}
      endContent={
        <HStack gap={2}>
          <Button label={MESSAGES.buttons.resume} size="sm" variant="primary" onClick={onResume} />
          {onRestart && <Button label={MESSAGES.buttons.restart} size="sm" onClick={onRestart} />}
        </HStack>
      }
    />
  )
}
