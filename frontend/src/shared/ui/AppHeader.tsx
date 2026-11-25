import { Button } from '@astryxdesign/core/Button'
import { HStack } from '@astryxdesign/core/HStack'
import { Text } from '@astryxdesign/core/Text'
import { TopNav, TopNavHeading } from '@astryxdesign/core/TopNav'
import { MESSAGES } from '../messages'

// 일반 화면(목록, 상세, 내 예매)의 머리: 왼쪽 글자 로고, 오른쪽 닉네임과 "내 예매".
export function AppHeader({ nickname, onHome, onMyBookings }: { nickname: string | null; onHome: () => void; onMyBookings: () => void }) {
  return (
    <TopNav
      heading={<TopNavHeading heading={MESSAGES.appName} />}
      endContent={
        nickname !== null && (
          <HStack gap={2} align="center">
            <Button label="공연" variant="ghost" onClick={onHome} />
            <Text color="secondary">{nickname}</Text>
            <Button label={MESSAGES.buttons.myBookings} variant="ghost" onClick={onMyBookings} />
          </HStack>
        )
      }
    />
  )
}
