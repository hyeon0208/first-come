import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'

// "‹ 제목": 앞 단계로 돌아가는 버튼이 붙은 화면 제목.
export function BackHeading({ title, backLabel, onBack, isDisabled = false }: { title: string; backLabel: string; onBack: () => void; isDisabled?: boolean }) {
  return (
    <HStack gap={1} align="center">
      <Button
        label={backLabel}
        isIconOnly
        variant="ghost"
        isDisabled={isDisabled}
        onClick={onBack}
        icon={
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M10 3L5 8l5 5" />
          </svg>
        }
      />
      <Heading level={1}>{title}</Heading>
    </HStack>
  )
}
