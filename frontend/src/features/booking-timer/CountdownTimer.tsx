import { HStack } from '@astryxdesign/core/HStack'
import { InfoTip } from '@astryxdesign/core/InfoTip'
import { Text } from '@astryxdesign/core/Text'
import { useEffect, useRef } from 'react'
import { useNow } from '../../shared/hooks/useNow'
import { MESSAGES } from '../../shared/messages'
import { formatRemaining } from '../../shared/time/format'

const LAST_MINUTE_MS = 60_000

// 끝나는 시각까지 남은 시간을 "9:48"로 보여 준다.
// - endsAt: 화면 시계 기준 끝나는 시각(밀리초)
// - notifyAtSeconds: 남은 시간이 이 초에 닿을 때 onNotify를 한 번씩 부른다(예: [180, 60])
// - onExpire: 0이 되면 한 번 부른다
// 숫자는 매초 읽어 주지 않는다(알림은 토스트가 맡는다).
export function CountdownTimer({
  label,
  endsAt,
  notifyAtSeconds = [],
  onNotify,
  onExpire,
}: {
  label: string
  endsAt: number
  notifyAtSeconds?: number[]
  onNotify?: (secondsLeft: number) => void
  onExpire?: () => void
}) {
  const now = useNow()
  const remaining = endsAt - now
  const fired = useRef<{ endsAt: number; marks: Set<number>; expired: boolean }>({ endsAt, marks: new Set(), expired: false })

  useEffect(() => {
    // 끝나는 시각이 바뀌면(다른 타이머로 넘어가면) 알림을 처음부터 다시 센다.
    if (fired.current.endsAt !== endsAt) fired.current = { endsAt, marks: new Set(), expired: false }
    const state = fired.current
    for (const mark of notifyAtSeconds) {
      // 이미 그 시점을 지나서 시작한 타이머는 지난 알림을 띄우지 않는다.
      if (!state.marks.has(mark) && remaining <= mark * 1000 && remaining > (mark - 2) * 1000) {
        state.marks.add(mark)
        onNotify?.(mark)
      }
    }
    if (!state.expired && remaining <= 0) {
      state.expired = true
      onExpire?.()
    }
  })

  return (
    <HStack gap={1} align="center">
      <Text color="secondary">{label}</Text>
      <span aria-hidden="true">
        <Text weight="bold" hasTabularNumbers color={remaining < LAST_MINUTE_MS ? 'inherit' : 'accent'}>
          <span style={remaining < LAST_MINUTE_MS ? { color: 'var(--color-text-error)' } : undefined}>{formatRemaining(remaining)}</span>
        </Text>
      </span>
      <InfoTip content={MESSAGES.timer.help} label="제한 시간 안내" />
    </HStack>
  )
}
