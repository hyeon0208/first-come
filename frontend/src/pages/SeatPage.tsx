import { BottomSheet } from '@astryxdesign/core/BottomSheet'
import { Button } from '@astryxdesign/core/Button'
import { Center } from '@astryxdesign/core/Center'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { Layout, LayoutContent, LayoutPanel } from '@astryxdesign/core/Layout'
import { Spinner } from '@astryxdesign/core/Spinner'
import { VStack } from '@astryxdesign/core/VStack'
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { stepPath, useBooking } from '../features/booking-shell'
import { PricePanel } from '../features/price'
import { SeatMap, SelectedSeatList, useSeatSelection } from '../features/seat-map'
import { api } from '../shared/api/endpoints'
import type { Seat, SeatGrade } from '../shared/api/types'
import { useMediaQuery } from '../shared/hooks/useMediaQuery'
import { MESSAGES } from '../shared/messages'
import { LoadFailed } from '../shared/ui/LoadFailed'
import { NoticeDialog } from '../shared/ui/NoticeDialog'

// 가선점이 이만큼 남았을 때 곧 풀린다고 경고한다
const PRESELECT_WARNING_MS = 120_000
const PANEL_WIDTH = 480

// 좌석 선택과 가격 선택. 배치도는 그대로 두고 오른쪽 칸만 바뀐다.
export function SeatPage() {
  const booking = useBooking()
  const navigate = useNavigate()
  const { runId, state, clock, seats, show, step } = booking
  const isPriceStep = step === 'price'
  const isWide = useMediaQuery('(min-width: 1024px)')
  const [isSheetOpen, setIsSheetOpen] = useState(false)
  const [isHolding, setIsHolding] = useState(false)
  // 경고를 띄운 가선점(끝나는 시각으로 구분한다). 그 가선점이 끝나거나 바뀌면 경고도 사라진다.
  const [warningFor, setWarningFor] = useState<number | null>(null)
  const warnedFor = useRef<number | null>(null)

  const selection = useSeatSelection({
    runId,
    mySeatIds: state.mySeatIds,
    preselectEndsAt: state.phase === 'SELECTING' ? clock.preselectEndsAt : null,
    applyState: booking.applyState,
    notify: booking.notify,
    reportError: booking.reportError,
    onSoldOut: booking.showSoldOut,
  })

  const mySeatIds = useMemo(() => new Set(state.mySeatIds), [state.mySeatIds])
  const mySeats = useMemo(() => seats.filter((seat) => mySeatIds.has(seat.seatId)), [seats, mySeatIds])

  // 첫 좌석을 누른 뒤 5분(2분 남음)에 경고한다. 가선점마다 한 번만 띄운다.
  useEffect(() => {
    const endsAt = clock.preselectEndsAt
    if (state.phase !== 'SELECTING' || endsAt === null || state.mySeatIds.length === 0) return
    const timer = window.setTimeout(
      () => {
        if (warnedFor.current !== endsAt) {
          warnedFor.current = endsAt
          setWarningFor(endsAt)
        }
      },
      Math.max(0, endsAt - PRESELECT_WARNING_MS - Date.now()),
    )
    return () => window.clearTimeout(timer)
  }, [clock.preselectEndsAt, state.phase, state.mySeatIds.length])

  // 선택 완료: 고른 좌석을 한 번에 선점한다. 못 잡은 좌석이 있으면 좌석 단계에 머문다.
  async function hold() {
    setWarningFor(null)
    setIsHolding(true)
    try {
      const result = await api.hold(runId)
      booking.applyState(result.state)
      if (result.unselectableSeatIds.length > 0) {
        booking.notify(MESSAGES.seat.partlyTaken)
      } else {
        booking.notify(MESSAGES.payTimeLeft(7))
        booking.go('price')
      }
    } catch (error) {
      booking.reportError(error, MESSAGES.seat.selectFailed)
    } finally {
      setIsHolding(false)
    }
  }

  async function submitPrices(quantities: { grade: SeatGrade; quantity: number }[]) {
    try {
      await api.validatePrices(runId, quantities)
      booking.go('order')
    } catch (error) {
      booking.reportError(error, MESSAGES.generalError)
    }
  }

  const panel: ReactNode = isPriceStep ? (
    // 뒤로 가기는 주소 이동으로 요청하고, 확인은 예매 화면 틀이 받는다(브라우저 뒤로 가기와 같은 길).
    <PricePanel seats={mySeats} onBack={() => navigate(stepPath(runId, 'seat'))} onSubmit={submitPrices} />
  ) : (
    <VStack gap={4} height="100%">
      <HStack justify="between" align="center">
        <Heading level={2}>{`선택 좌석 ${mySeats.length}`}</Heading>
        <Button label={MESSAGES.buttons.clearAll} variant="ghost" isDisabled={mySeats.length === 0} onClick={selection.clearAll} />
      </HStack>
      <SelectedSeatList seats={mySeats} isDisabled={selection.pendingSeatId !== null} onRemove={(seat: Seat) => selection.pressSeat(seat)} />
      <Button label={MESSAGES.buttons.selectDone} variant="primary" size="lg" isDisabled={mySeats.length === 0} isLoading={isHolding} onClick={hold} />
    </VStack>
  )

  const map = selection.hasLoadFailed ? (
    <Center height="100%">
      <LoadFailed message={MESSAGES.seat.loadFailed} onRetry={selection.refresh} />
    </Center>
  ) : !selection.hasLoaded ? (
    <Center height="100%">
      <Spinner label={MESSAGES.seat.loading} />
    </Center>
  ) : (
    <SeatMap
      seats={seats}
      prices={show.seatPrices}
      unavailableSeatIds={selection.unavailableSeatIds}
      mySeatIds={mySeatIds}
      pendingSeatId={selection.pendingSeatId}
      isInteractive={!isPriceStep}
      isRefreshing={selection.isRefreshing}
      onSeatPress={selection.pressSeat}
      onRefresh={selection.refresh}
    />
  )

  return (
    <>
      {isWide ? (
        <Layout
          content={<LayoutContent padding={4}>{map}</LayoutContent>}
          end={
            <LayoutPanel width={PANEL_WIDTH} hasDivider padding={4} label={isPriceStep ? '가격 선택' : '선택 좌석'}>
              {panel}
            </LayoutPanel>
          }
        />
      ) : (
        // 좁은 화면에서는 오른쪽 칸이 화면 아래 시트로 내려간다.
        <VStack height="100%" padding={3} gap={3}>
          {map}
          <Button
            label={isPriceStep ? '가격 선택' : `선택 좌석 ${mySeats.length}`}
            variant="primary"
            size="lg"
            onClick={() => setIsSheetOpen(true)}
          />
          <BottomSheet isOpen={isSheetOpen} onOpenChange={setIsSheetOpen} label={isPriceStep ? '가격 선택' : '선택 좌석'} padding={4}>
            {panel}
          </BottomSheet>
        </VStack>
      )}
      <NoticeDialog
        isOpen={warningFor !== null && warningFor === clock.preselectEndsAt && !isPriceStep && mySeats.length > 0}
        title={MESSAGES.preselectWarning.title}
        lines={[MESSAGES.preselectWarning.description]}
        actions={[{ label: MESSAGES.preselectWarning.action, onClick: hold }]}
      />
    </>
  )
}
