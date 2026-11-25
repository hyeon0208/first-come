import type { Seat, SeatGrade } from '../../shared/api/types'

export interface GradeGroup {
  grade: SeatGrade
  gradeName: string
  price: number
  seats: Seat[]
}

// 선점한 좌석을 등급별로 묶는다.
export function groupByGrade(seats: Seat[]): GradeGroup[] {
  const groups = new Map<SeatGrade, GradeGroup>()
  for (const seat of seats) {
    const group = groups.get(seat.grade) ?? { grade: seat.grade, gradeName: seat.gradeName, price: seat.price, seats: [] }
    group.seats.push(seat)
    groups.set(seat.grade, group)
  }
  return [...groups.values()]
}

// 등급마다 고른 수량이 그 등급의 좌석 수와 같아야 다음으로 간다.
export function isQuantityComplete(groups: GradeGroup[], quantities: Partial<Record<SeatGrade, number>>): boolean {
  return groups.every((group) => (quantities[group.grade] ?? 0) === group.seats.length)
}

export function ticketAmount(groups: GradeGroup[], quantities: Partial<Record<SeatGrade, number>>): number {
  return groups.reduce((sum, group) => sum + group.price * (quantities[group.grade] ?? 0), 0)
}
