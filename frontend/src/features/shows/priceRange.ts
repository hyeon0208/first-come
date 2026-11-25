import type { Show } from '../../shared/api/types'
import { formatWon } from '../../shared/time/format'

// "55,000원" 또는 "99,000원 ~ 132,000원"
export function priceRange(show: Show): string {
  const prices = show.seatPrices.map((price) => price.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  return min === max ? formatWon(min) : `${formatWon(min)} ~ ${formatWon(max)}`
}
