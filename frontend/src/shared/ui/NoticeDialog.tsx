import { Button } from '@astryxdesign/core/Button'
import { Dialog } from '@astryxdesign/core/Dialog'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'

interface NoticeAction {
  label: string
  onClick: () => void
  variant?: 'primary' | 'secondary'
}

// 버튼을 누르기 전에는 닫을 수 없는 안내 대화상자(시간 종료, 세션 만료, 매진 등).
export function NoticeDialog({
  isOpen,
  title,
  lines = [],
  actions,
}: {
  isOpen: boolean
  title: string
  lines?: string[]
  actions: NoticeAction[]
}) {
  return (
    <Dialog isOpen={isOpen} onOpenChange={() => undefined} purpose="required" padding={5}>
      <VStack gap={4}>
        <VStack gap={2}>
          <Heading level={2}>{title}</Heading>
          {lines.map((line) => (
            <Text key={line} as="p" color="secondary">
              {line}
            </Text>
          ))}
        </VStack>
        <HStack gap={2} justify="end">
          {actions.map((action) => (
            <Button key={action.label} label={action.label} variant={action.variant ?? 'primary'} onClick={action.onClick} />
          ))}
        </HStack>
      </VStack>
    </Dialog>
  )
}
