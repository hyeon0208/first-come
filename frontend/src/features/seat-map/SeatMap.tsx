import { HStack } from '@astryxdesign/core/HStack'
import { IconButton } from '@astryxdesign/core/IconButton'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { memo, useMemo, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'
import type { Seat, SeatPrice } from '../../shared/api/types'
import { MESSAGES } from '../../shared/messages'
import { formatWon } from '../../shared/time/format'
import { SEAT_GRADE_COLOR, SEAT_TOKENS } from '../../shared/ui/designTokens'
import { BLOCKS, HIT_SIZE, MAP_HEIGHT, MAP_WIDTH, SEAT_RADIUS, seatPosition, STAGE } from './seatLayout'

// 화면은 좌석을 세 가지로만 구분한다: 선택 가능, 선택 불가(판매됨과 남이 선점함을 구분하지 않는다), 내 선택.
export type SeatViewStatus = 'available' | 'unavailable' | 'mine'

const MIN_SCALE = 1
const MAX_SCALE = 4
const SCALE_STEP = 1.5
// 가격 단계에서 내 좌석 위치를 알리는 원의 지름
const HIGHLIGHT_DIAMETER = 36

interface View {
  scale: number
  x: number
  y: number
}

const FIT: View = { scale: 1, x: 0, y: 0 }

// SVG 좌석 배치도. 좌석은 원 하나씩이고, 좌석마다 button 역할과 이름을 갖는다.
// isInteractive가 false면(가격 단계) 좌석을 누를 수 없고 내 좌석 위치만 강조한다.
export function SeatMap({
  seats,
  prices,
  unavailableSeatIds,
  mySeatIds,
  pendingSeatId,
  isInteractive,
  isRefreshing,
  onSeatPress,
  onRefresh,
}: {
  seats: Seat[]
  prices: SeatPrice[]
  unavailableSeatIds: ReadonlySet<number>
  mySeatIds: ReadonlySet<number>
  pendingSeatId: number | null
  isInteractive: boolean
  isRefreshing: boolean
  onSeatPress: (seat: Seat) => void
  onRefresh: () => void
}) {
  const [view, setView] = useState<View>(FIT)
  const drag = useRef<{ pointerId: number; startX: number; startY: number; viewX: number; viewY: number; moved: boolean } | null>(null)
  const positioned = useMemo(() => seats.map((seat) => ({ seat, ...seatPosition(seat) })), [seats])

  function zoom(factor: number) {
    setView((current) => {
      const scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, current.scale * factor))
      // 가운데를 기준으로 키우고 줄인다.
      const ratio = scale / current.scale
      return clamp({ scale, x: MAP_WIDTH / 2 - (MAP_WIDTH / 2 - current.x) * ratio, y: MAP_HEIGHT / 2 - (MAP_HEIGHT / 2 - current.y) * ratio })
    })
  }

  function onPointerDown(event: PointerEvent<SVGSVGElement>) {
    drag.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, viewX: view.x, viewY: view.y, moved: false }
  }

  function onPointerMove(event: PointerEvent<SVGSVGElement>) {
    const state = drag.current
    if (!state || state.pointerId !== event.pointerId) return
    const svg = event.currentTarget
    const unit = MAP_WIDTH / svg.getBoundingClientRect().width
    const dx = (event.clientX - state.startX) * unit
    const dy = (event.clientY - state.startY) * unit
    if (Math.abs(dx) + Math.abs(dy) > 4) state.moved = true
    if (state.moved) setView((current) => clamp({ ...current, x: state.viewX + dx, y: state.viewY + dy }))
  }

  // 끌어서 옮긴 직후의 누름은 좌석 선택으로 치지 않는다.
  function press(seat: Seat) {
    if (drag.current?.moved) return
    onSeatPress(seat)
  }

  return (
    <VStack gap={2} height="100%">
      <SeatLegend prices={prices} />
      <HStack gap={2} align="end">
        <svg
          viewBox={`0 0 ${MAP_WIDTH} ${MAP_HEIGHT}`}
          role="group"
          aria-label="좌석 배치도"
          width="100%"
          style={{ ...SEAT_TOKENS, background: 'var(--seat-map-background)', touchAction: 'none', flex: 1, minWidth: 0 }}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={() => window.setTimeout(() => (drag.current = null), 0)}
          onPointerLeave={() => (drag.current = null)}
        >
          <g transform={`translate(${view.x} ${view.y}) scale(${view.scale})`}>
            <rect x={STAGE.x} y={STAGE.y} width={STAGE.width} height={STAGE.height} fill="var(--seat-stage)" rx="4" />
            <text x={STAGE.x + STAGE.width / 2} y={STAGE.y + STAGE.height / 2 + 5} textAnchor="middle" fontSize="14" fill="var(--color-text-primary)">
              STAGE
            </text>
            {BLOCKS.map((block) => (
              <g key={block.key}>
                <rect x={block.x - 8} y={block.y - 8} width={block.width + 16} height={block.height + 16} fill="var(--seat-map-floor)" rx="6" />
                <text x={block.x} y={block.y - 14} fontSize="13" fill="var(--color-text-secondary)">
                  {block.label}
                </text>
              </g>
            ))}
            {!isInteractive &&
              positioned
                .filter(({ seat }) => mySeatIds.has(seat.seatId))
                .map(({ seat, x, y }) => <circle key={seat.seatId} cx={x} cy={y} r={HIGHLIGHT_DIAMETER / 2} fill="var(--color-accent-muted)" />)}
            {positioned.map(({ seat, x, y }) => (
              <SeatDot
                key={seat.seatId}
                seat={seat}
                x={x}
                y={y}
                status={mySeatIds.has(seat.seatId) ? 'mine' : unavailableSeatIds.has(seat.seatId) ? 'unavailable' : 'available'}
                isPending={pendingSeatId === seat.seatId}
                isInteractive={isInteractive}
                onPress={press}
              />
            ))}
          </g>
        </svg>
        <VStack gap={1}>
          <IconButton label={MESSAGES.map.refresh} tooltip={MESSAGES.map.refresh} icon={<RefreshIcon />} isLoading={isRefreshing} isDisabled={!isInteractive} onClick={onRefresh} />
          <IconButton label={MESSAGES.map.zoomIn} tooltip={MESSAGES.map.zoomIn} icon={<PlusIcon />} onClick={() => zoom(SCALE_STEP)} />
          <IconButton label={MESSAGES.map.zoomOut} tooltip={MESSAGES.map.zoomOut} icon={<MinusIcon />} onClick={() => zoom(1 / SCALE_STEP)} />
          <IconButton label={MESSAGES.map.fit} tooltip={MESSAGES.map.fit} icon={<FitIcon />} onClick={() => setView(FIT)} />
        </VStack>
      </HStack>
    </VStack>
  )
}

// 배치도가 화면 밖으로 완전히 나가지 않게 한다.
function clamp(view: View): View {
  const minX = MAP_WIDTH - MAP_WIDTH * view.scale
  const minY = MAP_HEIGHT - MAP_HEIGHT * view.scale
  return { scale: view.scale, x: Math.min(0, Math.max(minX, view.x)), y: Math.min(0, Math.max(minY, view.y)) }
}

const STATUS_NAMES: Record<SeatViewStatus, string> = { available: '선택 가능', unavailable: MESSAGES.seat.unavailable, mine: '내 선택' }

const SeatDot = memo(function SeatDot({
  seat,
  x,
  y,
  status,
  isPending,
  isInteractive,
  onPress,
}: {
  seat: Seat
  x: number
  y: number
  status: SeatViewStatus
  isPending: boolean
  isInteractive: boolean
  onPress: (seat: Seat) => void
}) {
  const fill = status === 'mine' ? 'var(--color-accent)' : status === 'unavailable' ? 'var(--seat-unavailable)' : SEAT_GRADE_COLOR[seat.grade]
  const label = `${seat.name}, ${seat.gradeName}, ${STATUS_NAMES[status]}`

  function onKeyDown(event: KeyboardEvent<SVGGElement>) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onPress(seat)
    }
  }

  return (
    <g
      role="button"
      aria-label={label}
      aria-pressed={status === 'mine'}
      aria-disabled={!isInteractive || status === 'unavailable'}
      aria-busy={isPending}
      // 선택 불가 좌석은 Tab으로 건너뛴다.
      tabIndex={isInteractive && status !== 'unavailable' ? 0 : -1}
      data-seat-id={seat.seatId}
      data-status={status}
      onClick={isInteractive ? () => onPress(seat) : undefined}
      onKeyDown={isInteractive ? onKeyDown : undefined}
      style={{ cursor: isInteractive ? 'pointer' : 'default' }}
    >
      <circle cx={x} cy={y} r={SEAT_RADIUS} fill={fill} />
      {/* 내 좌석은 색만으로 구분하지 않도록 흰 체크 표시를 넣는다. */}
      {status === 'mine' && (
        <path d={`M ${x - 3} ${y} l 2 2 l 4 -4`} fill="none" stroke="var(--color-on-accent)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      )}
      {isPending && <circle cx={x} cy={y} r={SEAT_RADIUS + 2} fill="none" stroke="var(--color-accent)" strokeWidth="1.5" strokeDasharray="4 3" />}
      <rect x={x - HIT_SIZE / 2} y={y - HIT_SIZE / 2} width={HIT_SIZE} height={HIT_SIZE} fill="transparent" />
    </g>
  )
})

// 등급 색과 이름, 가격, 그리고 "선택 불가". 전석 공연은 두 등급의 이름이 같아 한 줄만 보인다.
function SeatLegend({ prices }: { prices: SeatPrice[] }) {
  const unique = prices.filter((price, index) => prices.findIndex((other) => other.name === price.name) === index)
  return (
    <HStack gap={3} wrap="wrap" align="center">
      {unique.map((price) => (
        <LegendItem key={price.name} color={SEAT_GRADE_COLOR[price.grade]} label={`${price.name} ${formatWon(price.price)}`} />
      ))}
      <LegendItem color="var(--seat-unavailable)" label={MESSAGES.seat.unavailable} />
    </HStack>
  )
}

function LegendItem({ color, label }: { color: string; label: string }) {
  return (
    <HStack gap={1} align="center">
      <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true" style={SEAT_TOKENS}>
        <circle cx="6" cy="6" r="6" fill={color} />
      </svg>
      <Text type="supporting">{label}</Text>
    </HStack>
  )
}

function iconProps() {
  return { width: 16, height: 16, viewBox: '0 0 16 16', fill: 'none', stroke: 'currentColor', strokeWidth: 1.6, strokeLinecap: 'round' as const, 'aria-hidden': true }
}

function RefreshIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M13 8a5 5 0 1 1-1.5-3.6" />
      <path d="M13 2.5v3h-3" />
    </svg>
  )
}

function PlusIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M8 3v10M3 8h10" />
    </svg>
  )
}

function MinusIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M3 8h10" />
    </svg>
  )
}

function FitIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M3 6V3h3M10 3h3v3M13 10v3h-3M6 13H3v-3" />
    </svg>
  )
}
