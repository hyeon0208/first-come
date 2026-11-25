import type { Performance } from '../../shared/api/types'

// 진행 중인 회차를 이어서 할 때 갈 곳. 아직 입장 전이면 그 공연의 상세로, 입장했으면 예매 화면으로 간다
// (예매 화면이 지금 단계에 맞는 화면으로 다시 보낸다).
export function resumePath(performance: Performance): string {
  return performance.enteredAt === null ? `/shows/${performance.showId}` : `/practice/${performance.id}/seat`
}
