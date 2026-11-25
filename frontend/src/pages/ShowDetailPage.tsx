import { Banner } from '@astryxdesign/core/Banner'
import { Calendar } from '@astryxdesign/core/Calendar'
import { Card } from '@astryxdesign/core/Card'
import { Grid } from '@astryxdesign/core/Grid'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { MetadataList, MetadataListItem } from '@astryxdesign/core/MetadataList'
import { SelectableCard } from '@astryxdesign/core/SelectableCard'
import { Skeleton } from '@astryxdesign/core/Skeleton'
import { Tab, TabList } from '@astryxdesign/core/TabList'
import { Text } from '@astryxdesign/core/Text'
import { useToast } from '@astryxdesign/core/Toast'
import { VStack } from '@astryxdesign/core/VStack'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { DifficultyPicker, InProgressBanner, OpenCountdownButton, openCountdownLabel, resumePath } from '../features/practice-start'
import { isApiError } from '../shared/api/client'
import { api } from '../shared/api/endpoints'
import type { Difficulty, Performance, Show } from '../shared/api/types'
import { useAsync } from '../shared/hooks/useAsync'
import { useNow } from '../shared/hooks/useNow'
import { MESSAGES } from '../shared/messages'
import { formatTime, formatWon, toIsoDate } from '../shared/time/format'
import { toClientTime } from '../shared/time/serverTime'
import { FakePoster } from '../shared/ui/FakePoster'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { PageFrame } from './PageFrame'
import { expectedShowDate, SHOW_START_TIME } from './usePerformanceDate'

type CalendarDate = NonNullable<Parameters<typeof Calendar>[0]['min']>

// 회차와, 그 회차의 예매 오픈 시각을 화면 시계로 옮긴 값.
interface StartedPerformance {
  performance: Performance
  openAt: number
}

function started(performance: Performance): StartedPerformance {
  return { performance, openAt: toClientTime(performance.openAt, performance.serverNow, Date.now()) }
}

export function ShowDetailPage() {
  const showId = Number(useParams().showId)
  const detail = useAsync(async () => {
    const [show, difficulties, current] = await Promise.all([api.getShow(showId), api.getDifficulties(), api.getCurrentPerformance()])
    return { show, difficulties, current: current ? started(current) : null }
  }, [showId])

  return (
    <PageFrame>
      {detail.status === 'loading' && (
        <Grid columns={{ minWidth: 320, max: 2 }} gap={5}>
          <Skeleton height={520} />
          <Skeleton height={520} index={1} />
        </Grid>
      )}
      {detail.status === 'error' && <LoadFailed message={MESSAGES.shows.failed} onRetry={detail.reload} />}
      {detail.status === 'ready' && <ShowDetail {...detail.data} onChanged={detail.reload} />}
    </PageFrame>
  )
}

function ShowDetail({
  show,
  difficulties,
  current: initialCurrent,
  onChanged,
}: {
  show: Show
  difficulties: Awaited<ReturnType<typeof api.getDifficulties>>
  current: StartedPerformance | null
  onChanged: () => void
}) {
  const navigate = useNavigate()
  const showToast = useToast()
  const [current, setCurrent] = useState(initialCurrent)
  const [difficulty, setDifficulty] = useState<Difficulty>(initialCurrent?.performance.difficulty ?? 'NORMAL')
  const [isCreating, setIsCreating] = useState(false)
  const [isFull, setIsFull] = useState(false)

  // 이 공연에서 막 시작해 아직 입장하지 않은 회차만 카운트다운으로 이어 보여 준다. 그 밖의 진행 중 회차는 띠로 알린다.
  const mine = current && current.performance.showId === show.id && current.performance.enteredAt === null ? current : null
  const other = current && !mine ? current : null
  const showDate = mine ? toIsoDate(mine.performance.startAt) : expectedShowDate()
  const showTime = mine ? formatTime(mine.performance.startAt) : SHOW_START_TIME
  // 공연일 하나만 고를 수 있고 이미 골라져 있다.
  const calendarDate = showDate as CalendarDate

  async function start() {
    setIsCreating(true)
    setIsFull(false)
    try {
      setCurrent(started(await api.createPerformance(show.id, difficulty)))
    } catch (error) {
      if (isApiError(error, 'PRACTICE_FULL')) setIsFull(true)
      else if (isApiError(error, 'ALREADY_IN_PROGRESS')) onChanged()
      else showToast({ body: MESSAGES.generalError, type: 'error' })
    } finally {
      setIsCreating(false)
    }
  }

  async function restart() {
    try {
      await api.abandonCurrentPerformance()
      setCurrent(null)
    } catch {
      showToast({ body: MESSAGES.generalError, type: 'error' })
    }
  }

  return (
    <Grid columns={{ minWidth: 320, max: 2 }} gap={5} align="start">
      <ShowInfo show={show} showDate={showDate} showTime={showTime} openAt={mine?.openAt ?? null} />
      <Card padding={4}>
        <VStack gap={4}>
          {other && <InProgressBanner onResume={() => navigate(resumePath(other.performance))} onRestart={restart} />}
          {isFull && <Banner status="warning" title={MESSAGES.practiceFull} collapsible={false} />}
          <Calendar mode="single" value={calendarDate} min={calendarDate} max={calendarDate} focusDate={calendarDate} />
          <SelectableCard label={showTime} isSelected onChange={() => undefined} padding={2}>
            <Text weight="semibold">{showTime}</Text>
          </SelectableCard>
          <Text color="secondary">{MESSAGES.noSeatCount}</Text>
          <DifficultyPicker difficulties={difficulties} value={difficulty} isLocked={isCreating || current !== null} onChange={setDifficulty} />
          <OpenCountdownButton
            openAt={mine?.openAt ?? null}
            isCreating={isCreating}
            isDisabled={other !== null}
            onStart={start}
            onBook={() => mine && navigate(`/practice/${mine.performance.id}/waiting`)}
          />
        </VStack>
      </Card>
    </Grid>
  )
}

function ShowInfo({ show, showDate, showTime, openAt }: { show: Show; showDate: string; showTime: string; openAt: number | null }) {
  const [tab, setTab] = useState('info')
  return (
    <VStack gap={4}>
      <HStack gap={4} align="start" wrap="wrap">
        <VStack width={220}>
          <FakePoster showId={show.id} title={show.title} />
        </VStack>
        <VStack gap={2}>
          <Heading level={1}>{show.title}</Heading>
          <Text color="secondary">{show.venueName}</Text>
          <OpenNotice openAt={openAt} />
        </VStack>
      </HStack>
      <TabList value={tab} onChange={setTab}>
        <Tab value="info" label="공연 정보" />
        <Tab value="price" label="가격" />
        <Tab value="guide" label="예매 안내" />
      </TabList>
      {tab === 'info' && (
        <MetadataList columns="single">
          <MetadataListItem label="장소">{show.venueName}</MetadataListItem>
          <MetadataListItem label="기간">{showDate.replaceAll('-', '.')}</MetadataListItem>
          <MetadataListItem label="시간">{`${showTime} (${show.runningMinutes}분)`}</MetadataListItem>
          <MetadataListItem label="연령">{show.ageLimit}</MetadataListItem>
        </MetadataList>
      )}
      {tab === 'price' && (
        <MetadataList columns="single">
          {uniquePrices(show).map((price) => (
            <MetadataListItem key={price.name} label={price.name}>
              {formatWon(price.price)}
            </MetadataListItem>
          ))}
        </MetadataList>
      )}
      {tab === 'guide' && (
        <MetadataList columns="single">
          <MetadataListItem label="좌석 선택 시간">10분</MetadataListItem>
          <MetadataListItem label="가선점">7분 (좌석은 누르는 순간 가선점)</MetadataListItem>
          <MetadataListItem label="예매 가능 시간">7분</MetadataListItem>
          <MetadataListItem label="매수 제한">1인 4매</MetadataListItem>
        </MetadataList>
      )}
    </VStack>
  )
}

// 예매 오픈 칸: "일반 예매 / 연습 시작 10초 뒤". 카운트다운 중에는 버튼과 같은 숫자를 보여 준다.
function OpenNotice({ openAt }: { openAt: number | null }) {
  const now = useNow()
  const text = openAt === null ? '연습 시작 10초 뒤' : now < openAt ? openCountdownLabel(openAt - now) : '예매 오픈'
  return (
    <Card padding={3} variant="muted">
      <HStack gap={2} align="center">
        <Text weight="semibold">일반 예매</Text>
        <Text color="secondary" hasTabularNumbers>
          {text}
        </Text>
      </HStack>
    </Card>
  )
}

// 전석 공연은 두 등급의 이름과 가격이 같다. 같은 이름은 한 줄로 보여 준다.
function uniquePrices(show: Show) {
  return show.seatPrices.filter((price, index, prices) => prices.findIndex((other) => other.name === price.name) === index)
}
