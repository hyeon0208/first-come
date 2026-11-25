import { formatRemaining } from '../../shared/time/format'

export function openCountdownLabel(remainingMs: number): string {
  return `예매 오픈까지 ${formatRemaining(remainingMs)}`
}
