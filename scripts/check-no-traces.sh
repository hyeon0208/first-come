#!/usr/bin/env bash
# 이 서비스에 없는 규칙과 이름이 저장소에 들어오지 않았는지 확인한다. 하나라도 있으면 실패한다.
set -euo pipefail
cd "$(dirname "$0")/.."

words=(
  'data''rize' 'assign''ment' 'com\.exam''ple' '과''제'
  '조''조' '청소년 할''인' '카드''사' '제''휴' '커플''석'
  'Card''Company' 'Discount''Policy' 'birth_?''date' 'seat_?''type' 'bundle_?''no' 'card_?''company' 'early_?''bird'
)
pattern=$(IFS='|'; echo "${words[*]}")

if git grep -nIiE "$pattern" -- . ':!frontend/package-lock.json'; then
  echo "실패: 위 위치에 금지어가 있습니다." >&2
  exit 1
fi
echo "통과: 금지어 0건"
