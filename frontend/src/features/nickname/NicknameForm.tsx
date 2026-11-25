import { Button } from '@astryxdesign/core/Button'
import { Card } from '@astryxdesign/core/Card'
import { Heading } from '@astryxdesign/core/Heading'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import { useState } from 'react'
import { api } from '../../shared/api/endpoints'
import type { User } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { isValidNickname } from './nicknameRule'

export function NicknameForm({ onCreated }: { onCreated: (user: User) => void }) {
  const [nickname, setNickname] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [hasFailed, setHasFailed] = useState(false)

  const isInvalid = nickname.length > 0 && !isValidNickname(nickname)
  const status = isInvalid
    ? { type: 'error' as const, message: MESSAGES.nickname.invalid }
    : hasFailed
      ? { type: 'error' as const, message: MESSAGES.nickname.failed }
      : undefined

  async function submit() {
    if (!isValidNickname(nickname) || isSubmitting) return
    setIsSubmitting(true)
    setHasFailed(false)
    try {
      onCreated(await api.createUser(nickname.trim()))
    } catch {
      setHasFailed(true)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card maxWidth={420} width="100%" padding={6}>
      <VStack gap={4}>
        <VStack gap={1}>
          <Heading level={1}>{MESSAGES.appName}</Heading>
          <Text color="secondary">{MESSAGES.intro}</Text>
        </VStack>
        <TextInput label="닉네임" value={nickname} onChange={setNickname} onEnter={submit} status={status} placeholder="2~12자" />
        <Button
          label={MESSAGES.buttons.start}
          variant="primary"
          isDisabled={!isValidNickname(nickname)}
          isLoading={isSubmitting}
          onClick={submit}
        />
      </VStack>
    </Card>
  )
}
