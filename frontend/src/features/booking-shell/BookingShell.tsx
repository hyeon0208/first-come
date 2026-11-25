import { AlertDialog } from '@astryxdesign/core/AlertDialog'
import { Button } from '@astryxdesign/core/Button'
import { Center } from '@astryxdesign/core/Center'
import { HStack } from '@astryxdesign/core/HStack'
import { Layout, LayoutContent, LayoutHeader } from '@astryxdesign/core/Layout'
import { Spinner } from '@astryxdesign/core/Spinner'
import { useToast } from '@astryxdesign/core/Toast'
import { TopNav, TopNavHeading } from '@astryxdesign/core/TopNav'
import { VStack } from '@astryxdesign/core/VStack'
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { Outlet, useBlocker, useLocation, useNavigate, useParams } from 'react-router'
import { isApiError } from '../../shared/api/client'
import { api } from '../../shared/api/endpoints'
import type { BookingState, Seat, Show } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatDateTime } from '../../shared/time/format'
import { LoadFailed } from '../../shared/ui/LoadFailed'
import { NoticeDialog } from '../../shared/ui/NoticeDialog'
import { CountdownTimer } from '../booking-timer'
import { useSession } from '../session'
import { BookingContext, type Booking } from './BookingContext'
import { activeTimer, stabilizeClock, timerNotice, toBookingClock, type BookingClock } from './bookingClock'
import { leaveKind, stepOf, stepPath, type LeaveKind, type Step } from './steps'

interface Loaded {
  show: Show
  seats: Seat[]
  startAt: string
}

type Notice =
  | { kind: 'seatTimeOver' }
  | { kind: 'bookingTimeOver'; isPaymentCancelled: boolean }
  | { kind: 'sessionExpired' }
  | { kind: 'soldOut' }
  | { kind: 'seatLost' }
  | { kind: 'backBlocked' }

// 화면 타이머가 0이 된 직후에는 서버 시계가 아직 그 시각을 지나지 않았을 수 있다. 조금 기다렸다가 끝난 것으로 본다.
const EXPIRY_MARGIN_MS = 300

// 예매 화면(좌석~결제)의 틀. 머리의 타이머, 나가기·뒤로 가기 확인, 시간 종료·세션 만료 안내를 한 곳에서 맡는다.
export function BookingShell() {
  const runId = Number(useParams().runId)
  const navigate = useNavigate()
  const location = useLocation()
  const showToast = useToast()
  const { session } = useSession()

  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [hasLoadFailed, setHasLoadFailed] = useState(false)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [state, setState] = useState<BookingState | null>(null)
  const [clock, setClock] = useState<BookingClock | null>(null)
  const [isPaying, setPaying] = useState(false)
  const [notice, setNotice] = useState<Notice | null>(null)
  const [pendingLeave, setPendingLeave] = useState<LeaveKind | null>(null)
  const [isLeaving, setIsLeaving] = useState(false)

  const step = stepOf(runId, location.pathname, location.search) ?? 'seat'
  // 주소 이동을 막을지 정하는 콜백이 항상 최신 값을 보도록 ref에 둔다.
  const latest = useRef({ step, clock, isPaying, phase: state?.phase, ended: false, allowNext: false })
  useLayoutEffect(() => {
    latest.current.step = step
    latest.current.phase = state?.phase
    latest.current.clock = clock
    latest.current.isPaying = isPaying
  }, [step, clock, isPaying, state?.phase])

  const notify = useCallback((message: string, type: 'info' | 'error' = 'info') => showToast({ body: message, type }), [showToast])

  // receivedAt은 그 상태를 서버에서 받은 화면 시각이다.
  const applyState = useCallback((next: BookingState, receivedAt: number = Date.now()) => {
    setState(next)
    setClock((previous) => stabilizeClock(previous, toBookingClock(next, receivedAt)))
  }, [])

  // 묻지 않고 이동한다(우리 코드가 정한 이동).
  const move = useCallback(
    (to: string, replace = false) => {
      latest.current.allowNext = true
      navigate(to, { replace })
    },
    [navigate],
  )

  // 처음 들어올 때 한 번 불러온다: 내 예매 상태, 공연, 좌석 배치.
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const bookingState = await api.getBookingState(runId)
        // 뒤따르는 요청을 기다리기 전에 받은 시각을 적어 둔다. 늦게 적으면 타이머가 그만큼 길게 보인다.
        const receivedAt = Date.now()
        const [show, seatMap, performance] = await Promise.all([
          api.getShow(bookingState.showId),
          api.getSeatMap(bookingState.showId),
          api.getCurrentPerformance(),
        ])
        if (cancelled) return
        setLoaded({ show, seats: seatMap.seats, startAt: performance?.startAt ?? bookingState.serverNow })
        applyState(bookingState, receivedAt)
      } catch (error) {
        if (cancelled) return
        if (isApiError(error, 'NOT_ENTERED')) move(`/practice/${runId}/waiting`, true)
        else if (isApiError(error, 'SESSION_EXPIRED', 'TIME_OVER')) setNotice({ kind: 'sessionExpired' })
        else setHasLoadFailed(true)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [runId, loadAttempt, applyState, move])

  // 지금 단계와 주소가 어긋나면 맞는 화면으로 보낸다(새로고침, 이어서 하기, 주소를 직접 친 경우).
  useEffect(() => {
    if (!state) return
    if (state.phase === 'RESERVED') {
      latest.current.ended = true
      move(`/practice/${runId}/complete?showId=${state.showId}`, true)
    } else if (state.phase === 'SELECTING' && step !== 'seat') {
      move(stepPath(runId, 'seat'), true)
    } else if (state.phase !== 'SELECTING' && step === 'seat') {
      move(stepPath(runId, state.phase === 'PAYING' ? 'pay' : 'price'), true)
    }
  }, [state, step, runId, move])

  // 다른 주소로 가려 할 때: 우리가 정한 이동은 그대로 두고, 그 밖에는 단계에 맞는 확인을 받는다.
  const blocker = useBlocker(({ nextLocation }) => {
    const current = latest.current
    if (current.allowNext) {
      current.allowNext = false
      return false
    }
    if (current.ended) return false
    if (current.isPaying) return true
    const seatSelectionEndsAt = current.clock?.seatSelectionEndsAt ?? null
    const isSeatSelectionOver = seatSelectionEndsAt !== null && Date.now() > seatSelectionEndsAt
    return leaveKind(current.step, stepOf(runId, nextLocation.pathname, nextLocation.search), isSeatSelectionOver) !== 'allow'
  })

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    // 결제 중에는 어디로도 가지 않는다.
    if (latest.current.isPaying) {
      blocker.reset()
      return
    }
    const seatSelectionEndsAt = latest.current.clock?.seatSelectionEndsAt ?? null
    const kind = leaveKind(
      latest.current.step,
      stepOf(runId, blocker.location.pathname, blocker.location.search),
      seatSelectionEndsAt !== null && Date.now() > seatSelectionEndsAt,
    )
    if (kind === 'backBlocked') {
      blocker.reset()
      setNotice({ kind: 'backBlocked' })
    } else {
      setPendingLeave(kind)
    }
  }, [blocker, runId])

  function cancelLeave() {
    // 종료 요청을 보내는 중에는 취소할 수 없다.
    if (isLeaving) return
    setPendingLeave(null)
    if (blocker.state === 'blocked') blocker.reset()
  }

  async function confirmLeave() {
    if (blocker.state !== 'blocked' || pendingLeave === null) return
    setIsLeaving(true)
    try {
      if (pendingLeave === 'exit') {
        await api.abandonCurrentPerformance()
        latest.current.ended = true
      } else if (pendingLeave === 'backToSeat') {
        applyState(await api.releaseHold(runId))
      }
      setPendingLeave(null)
      blocker.proceed()
    } catch (error) {
      setPendingLeave(null)
      blocker.reset()
      if (isApiError(error, 'BACK_BLOCKED')) setNotice({ kind: 'backBlocked' })
      else reportError(error, MESSAGES.generalError)
    } finally {
      setIsLeaving(false)
    }
  }

  const reportError = useCallback(
    (error: unknown, fallbackMessage: string) => {
      if (isApiError(error, 'TIME_OVER')) {
        setNotice(latest.current.step === 'seat' ? { kind: 'seatTimeOver' } : { kind: 'bookingTimeOver', isPaymentCancelled: false })
      } else if (isApiError(error, 'SESSION_EXPIRED')) {
        setNotice({ kind: 'sessionExpired' })
      } else {
        notify(fallbackMessage, 'error')
      }
    },
    [notify],
  )

  // 끝난 회차에서 나간다. 서버가 아직 끝내지 않았으면(시계가 조금 어긋난 경우) 진행 중 회차를 한 번 더 물어 끝나게 한다.
  async function leaveEnded(to: string) {
    latest.current.ended = true
    try {
      if ((await api.getCurrentPerformance())?.id === runId) {
        await new Promise((resolve) => window.setTimeout(resolve, 1000))
        await api.getCurrentPerformance()
      }
    } catch {
      // 나가는 길이다. 확인에 실패해도 나간다.
    }
    setNotice(null)
    move(to, true)
  }

  const showId = state?.showId ?? loaded?.show.id
  const detailPath = showId === undefined ? '/' : `/shows/${showId}`

  const go = useCallback((next: Step) => move(stepPath(runId, next)), [move, runId])
  const complete = useCallback(() => {
    latest.current.ended = true
    move(`/practice/${runId}/complete?showId=${showId}`, true)
  }, [move, runId, showId])

  const booking = useMemo<Booking | null>(() => {
    if (!loaded || !state || !clock || session.status !== 'ready' || !session.user) return null
    return {
      runId,
      show: loaded.show,
      seats: loaded.seats,
      nickname: session.user.nickname,
      state,
      clock,
      step,
      isPaying,
      setPaying,
      applyState,
      go,
      complete,
      reportError,
      notify,
      showSoldOut: () => setNotice({ kind: 'soldOut' }),
      showTimeOver: (isPaymentCancelled: boolean) => setNotice({ kind: 'bookingTimeOver', isPaymentCancelled }),
      showSeatLost: () => setNotice({ kind: 'seatLost' }),
    }
  }, [loaded, state, clock, session, runId, step, isPaying, applyState, go, complete, reportError, notify])

  // 불러오기가 끝나 예매 화면을 그릴 수 있는가.
  const isReady = loaded !== null && state !== null && clock !== null && session.status === 'ready' && session.user !== null
  const timer = state && clock ? activeTimer(state, clock) : null

  function onTimerExpire() {
    // 결제 중에는 종료 안내를 띄우지 않는다. 성공인지 시간 초과인지는 서버의 확정 단계가 정한다.
    if (latest.current.isPaying || !timer) return
    window.setTimeout(() => {
      if (latest.current.isPaying || latest.current.ended) return
      // 그사이 선택 완료가 성공했으면 좌석 선택 시간은 더 보지 않는다(선점 중이면 예매 가능 시간을 끝까지 준다).
      if (timer.kind === 'seatSelection' && latest.current.phase !== 'SELECTING') return
      // 이미 떠 있는 안내(매진 등)는 덮어쓰지 않는다.
      setNotice((current) => current ?? (timer.kind === 'seatSelection' ? { kind: 'seatTimeOver' } : { kind: 'bookingTimeOver', isPaymentCancelled: false }))
    }, EXPIRY_MARGIN_MS)
  }

  return (
    <>
      <Layout
        header={
          <LayoutHeader>
            <TopNav
              heading={
                <TopNavHeading
                  heading={MESSAGES.appName}
                  subheading={loaded ? `${loaded.show.title} · ${formatDateTime(loaded.startAt)}` : undefined}
                />
              }
              endContent={
                <HStack gap={3} align="center" wrap="wrap">
                  {timer && (
                    <CountdownTimer
                      label={timer.label}
                      endsAt={timer.endsAt}
                      notifyAtSeconds={timer.notifyAtSeconds}
                      onNotify={(seconds) => notify(timerNotice(timer.kind, seconds))}
                      onExpire={onTimerExpire}
                    />
                  )}
                  <Button label={MESSAGES.buttons.myBookings} variant="ghost" isDisabled={isPaying} onClick={() => navigate('/me')} />
                  <Button label={MESSAGES.buttons.restart} isDisabled={isPaying} onClick={() => navigate(detailPath)} />
                </HStack>
              }
            />
          </LayoutHeader>
        }
        content={
          <LayoutContent padding={0}>
            {isReady ? (
              <BookingContext.Provider value={booking}>
                <Outlet />
              </BookingContext.Provider>
            ) : hasLoadFailed ? (
              <Center height="100%">
                <LoadFailed
                  message={MESSAGES.seat.loadFailed}
                  onRetry={() => {
                    setHasLoadFailed(false)
                    setLoadAttempt((value) => value + 1)
                  }}
                />
              </Center>
            ) : (
              <Center height="100%">
                <VStack gap={2} hAlign="center">
                  <Spinner label={MESSAGES.seat.loading} />
                </VStack>
              </Center>
            )}
          </LayoutContent>
        }
      />

      <AlertDialog
        isOpen={pendingLeave === 'exit'}
        onOpenChange={(isOpen) => !isOpen && cancelLeave()}
        title={MESSAGES.exit.title}
        description={MESSAGES.exit.description}
        cancelLabel={MESSAGES.exit.cancel}
        actionLabel={MESSAGES.exit.action}
        isActionLoading={isLeaving}
        onAction={confirmLeave}
      />
      <AlertDialog
        isOpen={pendingLeave === 'backToSeat'}
        onOpenChange={(isOpen) => !isOpen && cancelLeave()}
        title={MESSAGES.backToSeat.title}
        description={MESSAGES.backToSeat.description}
        cancelLabel={MESSAGES.backToSeat.cancel}
        actionLabel={MESSAGES.backToSeat.action}
        actionVariant="primary"
        isActionLoading={isLeaving}
        onAction={confirmLeave}
      />
      <AlertDialog
        isOpen={pendingLeave === 'backToPrice'}
        onOpenChange={(isOpen) => !isOpen && cancelLeave()}
        title={MESSAGES.backToPrice.title}
        description=" "
        cancelLabel={MESSAGES.backToPrice.cancel}
        actionLabel={MESSAGES.backToPrice.action}
        actionVariant="primary"
        onAction={confirmLeave}
      />

      <NoticeDialog
        isOpen={notice?.kind === 'seatTimeOver'}
        title={MESSAGES.seatTimeOver.title}
        lines={[MESSAGES.seatTimeOver.description]}
        actions={[{ label: MESSAGES.buttons.confirm, onClick: () => leaveEnded(detailPath) }]}
      />
      <NoticeDialog
        isOpen={notice?.kind === 'bookingTimeOver'}
        title={MESSAGES.bookingTimeOver.title}
        lines={[
          MESSAGES.bookingTimeOver.description,
          ...(notice?.kind === 'bookingTimeOver' && notice.isPaymentCancelled ? [MESSAGES.bookingTimeOver.paymentCancelled] : []),
        ]}
        actions={[{ label: MESSAGES.buttons.confirm, onClick: () => leaveEnded(detailPath) }]}
      />
      <NoticeDialog
        isOpen={notice?.kind === 'sessionExpired'}
        title={MESSAGES.sessionExpired}
        actions={[{ label: MESSAGES.buttons.confirm, onClick: () => leaveEnded(detailPath) }]}
      />
      <NoticeDialog
        isOpen={notice?.kind === 'soldOut'}
        title={MESSAGES.soldOut.title}
        lines={[MESSAGES.soldOut.description]}
        actions={[
          { label: MESSAGES.buttons.result, variant: 'secondary', onClick: () => leaveEnded('/me') },
          { label: MESSAGES.buttons.restart, onClick: () => leaveEnded(detailPath) },
        ]}
      />
      <NoticeDialog
        isOpen={notice?.kind === 'seatLost'}
        title={MESSAGES.seatLost}
        actions={[
          {
            label: MESSAGES.buttons.confirm,
            onClick: async () => {
              await api.abandonCurrentPerformance().catch(() => undefined)
              leaveEnded(detailPath)
            },
          },
        ]}
      />
      <NoticeDialog
        isOpen={notice?.kind === 'backBlocked'}
        title={MESSAGES.backBlocked.title}
        lines={[MESSAGES.backBlocked.description]}
        actions={[{ label: MESSAGES.backBlocked.action, onClick: () => setNotice(null) }]}
      />
    </>
  )
}
