#!/usr/bin/env bash
# 기준 부하 측정을 돌린다. 동시 연습 수마다 k6를 한 번씩 돌리고, 그동안 어디가 먼저 막히는지 볼 근거를 함께 남긴다.
#   - 컨테이너의 CPU와 메모리 (docker stats)
#   - 커넥션 풀: 쓰는 중인 연결 수와 연결을 기다리는 스레드 수 (백엔드의 /actuator/metrics)
#   - MySQL: 행 잠금을 기다린 횟수와 시간, 실행 중인 스레드 수 (SHOW GLOBAL STATUS)
#
#   load/run-baseline.sh                              # 동시 연습 5, 10, 20, 40개를 2분씩
#   VUS_LIST="1" DURATION=30s load/run-baseline.sh    # 스크립트가 도는지만 확인
#
# 먼저 서버를 띄운다. 동시 연습 상한(기본 5)은 측정하는 동안 풀어 둔다:
#   BOOKING_MAX_ACTIVE_PERFORMANCES=1000 docker compose up -d --build
set -euo pipefail
cd "$(dirname "$0")/.."

VUS_LIST=${VUS_LIST:-"5 10 20 40"}
DURATION=${DURATION:-2m}
# 입장 뒤 좌석을 누르기 전에 쉬는 초(사람처럼). 0이면 곧바로 누른다.
THINK_SECONDS=${THINK_SECONDS:-0}
NAME=$(date +%Y-%m-%d)-raw
OUT=load/results/$NAME
mkdir -p "$OUT"

metric() {
  curl -s -m 2 "localhost:8080/actuator/metrics/$1" | grep -o '"value":[0-9.E+-]*' | head -1 | cut -d: -f2
}

mysql_status() {
  docker compose exec -T mysql mysql -uroot -proot -N -e \
    "SHOW GLOBAL STATUS WHERE Variable_name IN ('Innodb_row_lock_waits', 'Innodb_row_lock_time', 'Innodb_deadlocks', 'Threads_running', 'Threads_connected')" 2>/dev/null | tr '\t' '=' | tr '\n' ' '
}

sample() {
  while true; do
    now=$(date +%H:%M:%S)
    docker stats --no-stream --format '{{.Name}} cpu={{.CPUPerc}} mem={{.MemUsage}}' | sed "s/^/$now /"
    echo "$now pool active=$(metric hikaricp.connections.active) pending=$(metric hikaricp.connections.pending) max=$(metric hikaricp.connections.max)"
    echo "$now mysql $(mysql_status)"
    sleep 5
  done
}

for vus in $VUS_LIST; do
  label="$vus-think$THINK_SECONDS"
  echo "== 동시 연습 ${vus}개, 입장 뒤 쉬는 시간 ${THINK_SECONDS}초"
  echo "시작 mysql $(mysql_status)" > "$OUT/samples-$label.txt"
  sample >> "$OUT/samples-$label.txt" &
  sample_pid=$!
  docker run --rm -i -v "$PWD/load:/load" -e VUS="$vus" -e DURATION="$DURATION" -e THINK_SECONDS="$THINK_SECONDS" -e SUMMARY_PATH="/load/results/$NAME/summary-$label.json" \
    grafana/k6 run --quiet /load/baseline.js | tee "$OUT/summary-$label.txt"
  kill "$sample_pid" 2>/dev/null || true
  wait "$sample_pid" 2>/dev/null || true
  echo "끝 mysql $(mysql_status)" >> "$OUT/samples-$label.txt"
  # 다음 단계 전에 남은 회차가 끝나기를 기다린다.
  sleep 20
done
echo "결과: $OUT"
