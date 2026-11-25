import type { DifficultyInfo } from '../../shared/api/types'

// "가상 경쟁자 500명 · 약 1분 40초에 매진"
export function difficultyDescription(info: DifficultyInfo): string {
  const count = `가상 경쟁자 ${info.competitorCount.toLocaleString('ko-KR')}명`
  if (info.soldOutSeconds === null) return `${count} · 매진되지 않음`
  const minutes = Math.floor(info.soldOutSeconds / 60)
  const seconds = info.soldOutSeconds % 60
  const time = [minutes > 0 ? `${minutes}분` : '', seconds > 0 ? `${seconds}초` : ''].filter(Boolean).join(' ')
  return `${count} · 약 ${time}에 매진`
}
