import type { Seat } from '../../shared/api/types'

// 좌석 배치도의 좌표. 좌석 원은 지름 12, 좌석 사이 4라 한 칸이 16이다.
export const SEAT_RADIUS = 6
export const SEAT_PITCH = 16
// 누르는 영역(투명 사각)
export const HIT_SIZE = 16

const BLOCK_GAP = 48
const FLOOR_ONE_COLUMNS = 16
const FLOOR_ONE_ROWS = 12
const FLOOR_TWO_COLUMNS = 20
const FLOOR_TWO_ROWS = 4
const MARGIN = 40
const STAGE_HEIGHT = 36
const LABEL_HEIGHT = 28

const FLOOR_ONE_BLOCK_WIDTH = FLOOR_ONE_COLUMNS * SEAT_PITCH
const FLOOR_ONE_BLOCK_HEIGHT = FLOOR_ONE_ROWS * SEAT_PITCH
const FLOOR_TWO_BLOCK_WIDTH = FLOOR_TWO_COLUMNS * SEAT_PITCH
const FLOOR_TWO_BLOCK_HEIGHT = FLOOR_TWO_ROWS * SEAT_PITCH

export const MAP_WIDTH = MARGIN * 2 + FLOOR_TWO_BLOCK_WIDTH * 3 + BLOCK_GAP * 2
const FLOOR_ONE_LEFT = (MAP_WIDTH - (FLOOR_ONE_BLOCK_WIDTH * 2 + BLOCK_GAP)) / 2
const FRONT_TOP = MARGIN + STAGE_HEIGHT + LABEL_HEIGHT + 12
const BACK_TOP = FRONT_TOP + FLOOR_ONE_BLOCK_HEIGHT + LABEL_HEIGHT + 20
const FLOOR_TWO_TOP = BACK_TOP + FLOOR_ONE_BLOCK_HEIGHT + LABEL_HEIGHT + 36
export const MAP_HEIGHT = FLOOR_TWO_TOP + FLOOR_TWO_BLOCK_HEIGHT + MARGIN

export const STAGE = { x: FLOOR_ONE_LEFT, y: MARGIN, width: FLOOR_ONE_BLOCK_WIDTH * 2 + BLOCK_GAP, height: STAGE_HEIGHT }

export interface BlockArea {
  key: string
  label: string
  x: number
  y: number
  width: number
  height: number
}

// 1층은 무대 쪽에 A·B, 그 뒤에 C·D가 놓이고, 2층 A·B·C가 가장 뒤다.
export const BLOCKS: BlockArea[] = [
  floorOne('A', 0, FRONT_TOP),
  floorOne('B', 1, FRONT_TOP),
  floorOne('C', 0, BACK_TOP),
  floorOne('D', 1, BACK_TOP),
  floorTwo('A', 0),
  floorTwo('B', 1),
  floorTwo('C', 2),
]

function floorOne(block: string, column: number, top: number): BlockArea {
  return {
    key: `1-${block}`,
    label: `1층 ${block}구역`,
    x: FLOOR_ONE_LEFT + column * (FLOOR_ONE_BLOCK_WIDTH + BLOCK_GAP),
    y: top,
    width: FLOOR_ONE_BLOCK_WIDTH,
    height: FLOOR_ONE_BLOCK_HEIGHT,
  }
}

function floorTwo(block: string, column: number): BlockArea {
  return {
    key: `2-${block}`,
    label: `2층 ${block}구역`,
    x: MARGIN + column * (FLOOR_TWO_BLOCK_WIDTH + BLOCK_GAP),
    y: FLOOR_TWO_TOP,
    width: FLOOR_TWO_BLOCK_WIDTH,
    height: FLOOR_TWO_BLOCK_HEIGHT,
  }
}

const BLOCK_BY_KEY = new Map(BLOCKS.map((block) => [block.key, block]))

// 좌석 원의 가운데 좌표.
export function seatPosition(seat: Seat): { x: number; y: number } {
  const block = BLOCK_BY_KEY.get(`${seat.floor}-${seat.block}`)
  if (!block) throw new Error(`배치도에 없는 구역입니다: ${seat.name}`)
  return {
    x: block.x + (seat.seatNo - 1) * SEAT_PITCH + SEAT_PITCH / 2,
    y: block.y + (seat.rowNo - 1) * SEAT_PITCH + SEAT_PITCH / 2,
  }
}
