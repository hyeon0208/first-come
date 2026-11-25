import { SelectableCard } from '@astryxdesign/core/SelectableCard'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import type { Difficulty, DifficultyInfo } from '../../shared/api/types'
import { DIFFICULTY_NAMES } from '../../shared/messages'
import { difficultyDescription } from './difficultyDescription'

export function DifficultyPicker({
  difficulties,
  value,
  isLocked,
  onChange,
}: {
  difficulties: DifficultyInfo[]
  value: Difficulty
  isLocked: boolean
  onChange: (difficulty: Difficulty) => void
}) {
  return (
    <VStack gap={2}>
      <Text type="label">난이도</Text>
      {difficulties.map((info) => (
        <SelectableCard
          key={info.difficulty}
          label={DIFFICULTY_NAMES[info.difficulty]}
          isSelected={value === info.difficulty}
          isDisabled={isLocked}
          onChange={() => onChange(info.difficulty)}
          padding={3}
        >
          <VStack gap={0.5}>
            <Text weight="semibold">{DIFFICULTY_NAMES[info.difficulty]}</Text>
            <Text color="secondary" type="supporting">
              {difficultyDescription(info)}
            </Text>
          </VStack>
        </SelectableCard>
      ))}
    </VStack>
  )
}
