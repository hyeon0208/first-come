#!/usr/bin/env bash
# 기준 부하 측정을 돌린다. 동시 연습 수마다 k6를 한 번씩 돌리고, 그동안의 컨테이너 자원 사용량을 함께 남긴다.
#
#   load/run-baseline.sh                 # 동시 연습 5, 10, 20, 40개를 2분씩
#   VUS_LIST="1" DURATION=30s load/run-baseline.sh   # 스크립트가 도는지만 확인
#
# 먼저 서버를 띄운다. 동시 연습 상한(기본 10)은 측정하는 동안 풀어 둔다:
#   BOOKING_MAX_ACTIVE_PERFORMANCES=1000 docker compose up -d --build
set -euo pipefail
cd "$(dirname "$0")/.."

VUS_LIST=${VUS_LIST:-"5 10 20 40"}
DURATION=${DURATION:-2m}
OUT=load/results/$(date +%Y-%m-%d)-raw
mkdir -p "$OUT"

for vus in $VUS_LIST; do
  echo "== 동시 연습 ${vus}개"
  # 5초마다 컨테이너의 CPU와 메모리를 적는다.
  ( while true; do docker stats --no-stream --format '{{.Name}} {{.CPUPerc}} {{.MemUsage}}' | sed "s/^/$(date +%H:%M:%S) /"; sleep 5; done ) > "$OUT/stats-$vus.txt" &
  stats_pid=$!
  docker run --rm -i -v "$PWD/load:/load" -e VUS="$vus" -e DURATION="$DURATION" -e SUMMARY_PATH="/load/results/$(basename "$OUT")/summary-$vus.json" \
    grafana/k6 run --quiet /load/baseline.js | tee "$OUT/summary-$vus.txt"
  kill "$stats_pid" 2>/dev/null || true
  wait "$stats_pid" 2>/dev/null || true
  # 다음 단계 전에 남은 회차가 끝나기를 기다린다.
  sleep 20
done
echo "결과: $OUT"
