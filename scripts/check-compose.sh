#!/usr/bin/env bash
# docker compose 한 번으로 서버, 화면, 모의 결제 서버, MySQL이 뜨는지 확인한다.
# 다른 프로젝트 이름으로 띄우려면: COMPOSE_PROJECT_NAME=<이름> scripts/check-compose.sh
set -euo pipefail
cd "$(dirname "$0")/.."

fail() {
  echo "실패: $1" >&2
  exit 1
}

docker compose up -d --build

echo "백엔드가 뜨기를 기다린다"
health=""
for _ in $(seq 1 60); do
  health=$(curl -s -m 2 localhost:8080/actuator/health || true)
  [[ "$health" == *'"status":"UP"'* ]] && break
  sleep 2
done
[[ "$health" == *'"status":"UP"'* ]] || fail "백엔드 헬스가 UP이 아니다: $health"

for service in mysql backend payment-mock frontend; do
  state=$(docker compose ps --format '{{.Service}} {{.State}}' | awk -v s="$service" '$1 == s { print $2 }')
  [[ "$state" == "running" ]] || fail "$service 상태가 running이 아니다: ${state:-없음}"
done

[[ "$(curl -s -o /dev/null -w '%{http_code}' -m 5 localhost:3000/)" == "200" ]] || fail "화면이 200으로 답하지 않는다"
# 화면 주소의 /api 요청이 백엔드까지 닿고, MySQL의 고정 데이터(공연 3개)가 읽히는지 본다.
shows=$(curl -s -m 5 localhost:3000/api/v1/shows | grep -o '"id":' | wc -l | tr -d ' ')
[[ "$shows" == "3" ]] || fail "공연이 3개가 아니다: $shows"
# 모의 결제 서버: 승인이 없는 주문의 취소도 성공으로 답한다.
cancel=$(curl -s -m 5 -H 'Content-Type: application/json' -d '{"orderId":0}' localhost:8081/payments/cancel)
[[ "$cancel" == *'"cancelled":false'* ]] || fail "모의 결제 서버가 답하지 않는다: $cancel"
# 장애 주입 경로는 열려 있지 않아야 한다.
[[ "$(curl -s -o /dev/null -w '%{http_code}' -m 5 -X PUT -H 'Content-Type: application/json' -d '{"mode":"DECLINE"}' localhost:8081/test/faults)" == "404" ]] ||
  fail "모의 결제 서버의 장애 주입 경로가 열려 있다"

echo "통과: 네 서비스가 떠 있고 화면, API, 모의 결제 서버가 답한다"
