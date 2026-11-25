import { Card } from '@astryxdesign/core/Card'
import { Center } from '@astryxdesign/core/Center'
import { Heading } from '@astryxdesign/core/Heading'
import { ProgressBar } from '@astryxdesign/core/ProgressBar'
import { Text } from '@astryxdesign/core/Text'
import { useToast } from '@astryxdesign/core/Toast'
import { VStack } from '@astryxdesign/core/VStack'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { isApiError } from '../shared/api/client'
import { api } from '../shared/api/endpoints'
import { MESSAGES } from '../shared/messages'
import { LoadFailed } from '../shared/ui/LoadFailed'

// 대기열 화면. 지금은 줄을 세우지 않는다: 1초 보여 주고 예매 화면으로 넘긴다.
const WAITING_DISPLAY_MS = 1000

export function WaitingPage() {
  const runId = Number(useParams().runId)
  const navigate = useNavigate()
  const showToast = useToast()
  const [hasFailed, setHasFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    const timer = window.setTimeout(async () => {
      try {
        await api.enter(runId)
        if (!cancelled) navigate(`/practice/${runId}/seat`, { replace: true })
      } catch (error) {
        if (cancelled) return
        // 끝났거나 남의 회차면 처음으로 보낸다.
        if (isApiError(error, 'SESSION_EXPIRED', 'TIME_OVER')) {
          navigate('/', { replace: true })
        } else if (isApiError(error, 'NOT_OPEN_YET')) {
          // 화면 시계가 서버보다 빨랐다. 알리고 상세의 카운트다운으로 돌아간다.
          showToast({ body: MESSAGES.notOpenYet })
          const current = await api.getCurrentPerformance().catch(() => null)
          if (!cancelled) navigate(current ? `/shows/${current.showId}` : '/', { replace: true })
        } else {
          setHasFailed(true)
        }
      }
    }, WAITING_DISPLAY_MS)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [runId, attempt, navigate, showToast])

  return (
    <Center minHeight="100dvh" padding={4}>
      <Card maxWidth={420} width="100%" padding={6}>
        {hasFailed ? (
          <LoadFailed message={MESSAGES.waiting.failed} onRetry={() => {
              setHasFailed(false)
              setAttempt((value) => value + 1)
            }} />
        ) : (
          <VStack gap={4} hAlign="center">
            <Text color="secondary">나의 대기순서</Text>
            <Heading level={1} type="display-2">
              0
            </Heading>
            <ProgressBar label="대기 진행" isLabelHidden value={100} />
            <Text color="secondary">현재 대기인원 0명</Text>
            <Text color="secondary" justify="center">
              {MESSAGES.waiting.notice}
            </Text>
          </VStack>
        )}
      </Card>
    </Center>
  )
}
