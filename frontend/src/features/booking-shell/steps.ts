// 예매 화면의 단계. 주소로 구분한다: 좌석 → 가격(좌석 화면의 ?step=price) → 예매 정보 → 결제.
export type Step = 'seat' | 'price' | 'order' | 'pay'

export function stepPath(runId: number, step: Step): string {
  const base = `/practice/${runId}`
  if (step === 'seat') return `${base}/seat`
  if (step === 'price') return `${base}/seat?step=price`
  return `${base}/${step}`
}

// 이 회차의 예매 화면 주소가 아니면 null.
export function stepOf(runId: number, pathname: string, search: string): Step | null {
  const base = `/practice/${runId}/`
  if (!pathname.startsWith(base)) return null
  const rest = pathname.slice(base.length)
  if (rest === 'seat') return new URLSearchParams(search).get('step') === 'price' ? 'price' : 'seat'
  if (rest === 'order' || rest === 'pay') return rest
  return null
}

export type LeaveKind = 'allow' | 'exit' | 'backToSeat' | 'backBlocked' | 'backToPrice'

// 지금 단계에서 다른 주소로 가려 할 때 무엇을 물어야 하는가.
// - 예매 화면 밖으로: 예매를 종료할지 묻는다
// - 가격 단계 뒤에서 좌석 단계로: 좌석이 풀린다고 알리고 묻는다. 좌석 선택 시간이 끝났으면 갈 수 없다
// - 예매 정보에서 가격으로: 작성 중인 정보가 사라진다고 묻는다
// - 결제에서 예매 정보로: 묻지 않는다
export function leaveKind(current: Step, next: Step | null, isSeatSelectionOver: boolean): LeaveKind {
  if (next === null) return 'exit'
  if (next === 'seat' && current !== 'seat') return isSeatSelectionOver ? 'backBlocked' : 'backToSeat'
  if (next === 'price' && current === 'order') return 'backToPrice'
  return 'allow'
}
