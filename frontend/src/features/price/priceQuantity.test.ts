import { describe, expect, it } from 'vitest'
import type { Seat } from '../../shared/api/types'
import { groupByGrade, isQuantityComplete, ticketAmount } from './priceQuantity'

function seat(seatId: number, grade: 'R' | 'S', price: number): Seat {
  return { seatId, floor: 1, block: 'A', rowNo: 1, seatNo: seatId, name: `1층 A구역 1열 ${seatId}번`, grade, gradeName: `${grade}석`, price }
}

describe('가격 단계의 수량', () => {
  const groups = groupByGrade([seat(1, 'R', 132_000), seat(2, 'R', 132_000), seat(400, 'S', 99_000)])

  it('선점한 좌석을 등급별로 묶는다', () => {
    expect(groups.map((group) => [group.gradeName, group.seats.length])).toEqual([
      ['R석', 2],
      ['S석', 1],
    ])
  })

  it('등급마다 수량이 좌석 수와 같아야 다음으로 간다', () => {
    expect(isQuantityComplete(groups, {})).toBe(false)
    expect(isQuantityComplete(groups, { R: 2 })).toBe(false)
    expect(isQuantityComplete(groups, { R: 1, S: 2 })).toBe(false)
    expect(isQuantityComplete(groups, { R: 2, S: 1 })).toBe(true)
  })

  it('티켓 금액은 등급 가격에 수량을 곱해 더한다', () => {
    expect(ticketAmount(groups, { R: 2, S: 1 })).toBe(363_000)
  })
})
