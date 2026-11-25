// 기준 부하 측정: 동시에 진행되는 연습 수를 올려 가며 처리량, 응답 시간, 먼저 막히는 곳을 본다.
// 가상 사용자 한 명이 연습 한 번을 처음부터 끝까지 한다:
//   닉네임 → 연습 시작(어려움) → 오픈까지 대기 → 입장 → 좌석 상태 → 좌석 누르기(거절되면 다른 좌석) → 선택 완료 → 가격 확인 → 금액 → 결제
// 어려움 난이도라 연습마다 가상 경쟁자 1,000명이 서버 안에서 함께 움직인다. 부하의 대부분은 그쪽에서 나온다.
//
// 실행은 load/run-baseline.sh 가 한다(동시 연습 수마다 한 번씩).
import { check, sleep } from 'k6'
import http from 'k6/http'
import { Counter, Trend } from 'k6/metrics'

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080'
const API = `${BASE}/api/v1`
const VUS = Number(__ENV.VUS || 1)
const DURATION = __ENV.DURATION || '2m'
const DIFFICULTY = __ENV.DIFFICULTY || 'HARD'
const WANTED_SEATS = 2
const MAX_PRESSES = 12

export const options = {
  scenarios: {
    practice: { executor: 'constant-vus', vus: VUS, duration: DURATION, gracefulStop: '40s' },
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
}

// 연습이 끝난 방식별 횟수
const outcomes = {
  success: new Counter('practice_success'),
  soldOut: new Counter('practice_sold_out'),
  full: new Counter('practice_rejected_full'),
  failed: new Counter('practice_failed'),
}
const seatRejected = new Counter('seat_rejected')
// 연습 한 번에서 좌석을 누르기 시작해 결제가 끝날 때까지
const bookingDuration = new Trend('booking_duration', true)

function json(response) {
  try {
    return response.json()
  } catch (_) {
    return null
  }
}

function post(path, body, headers, name) {
  return http.post(`${API}${path}`, body === null ? null : JSON.stringify(body), {
    headers: Object.assign({ 'Content-Type': 'application/json' }, headers),
    tags: { name },
  })
}

function get(path, headers, name) {
  return http.get(`${API}${path}`, { headers, tags: { name } })
}

// 서버가 준 두 시각(시간대 표기 없음)의 차이(초)
function secondsBetween(from, to) {
  return (Date.parse(`${to.slice(0, 23)}Z`) - Date.parse(`${from.slice(0, 23)}Z`)) / 1000
}

export function setup() {
  const seatMap = json(http.get(`${API}/shows/1/seat-map`))
  if (!seatMap) throw new Error(`서버에 닿지 못했다: ${API}`)
  return { gradeBySeatId: Object.fromEntries(seatMap.seats.map((seat) => [seat.seatId, seat.grade])) }
}

export default function (data) {
  // 가상 사용자마다, 연습마다 새 닉네임으로 시작한다(쿠키는 k6가 가상 사용자별로 들고 있다).
  http.cookieJar().clear(BASE)
  const user = post('/users', { nickname: `부하${__VU}` }, {}, 'POST /users')
  if (!check(user, { '닉네임 등록 201': (r) => r.status === 201 })) return fail()

  const created = post('/performances', { showId: 1, difficulty: DIFFICULTY }, {}, 'POST /performances')
  if (created.status === 503) {
    outcomes.full.add(1)
    sleep(1)
    return
  }
  if (!check(created, { '연습 시작 201': (r) => r.status === 201 })) return fail()
  const performance = json(created)
  const run = { 'X-Practice-Run': String(performance.id) }

  // 예매 오픈 시각까지 기다린다.
  sleep(Math.max(0, secondsBetween(performance.serverNow, performance.openAt)) + 0.1)
  const entered = post('/booking/enter', null, run, 'POST /booking/enter')
  if (!check(entered, { '입장 200': (r) => r.status === 200 })) return abandon()

  const startedAt = Date.now()
  const statuses = json(get('/booking/seats', run, 'GET /booking/seats'))
  if (!statuses) return abandon()
  if (statuses.soldOut) return soldOut()

  // 선택 가능으로 보이는 좌석을 무작위로 누른다. 가상 경쟁자가 먼저 잡았으면 서버가 거절한다.
  const taken = new Set(statuses.unavailableSeatIds)
  const candidates = Object.keys(data.gradeBySeatId)
    .map(Number)
    .filter((seatId) => !taken.has(seatId))
  const mine = []
  for (let press = 0; press < MAX_PRESSES && mine.length < WANTED_SEATS && candidates.length > 0; press++) {
    const seatId = candidates.splice(Math.floor(Math.random() * candidates.length), 1)[0]
    const pressed = post(`/booking/seats/${seatId}/preselect`, null, run, 'POST /booking/seats/{id}/preselect')
    if (pressed.status === 200) {
      mine.push(seatId)
    } else if (pressed.status === 409) {
      seatRejected.add(1)
      if (json(pressed)?.code === 'SOLD_OUT') return soldOut()
    } else {
      return abandon()
    }
  }
  if (mine.length === 0) return abandon()

  const held = post('/booking/hold', null, run, 'POST /booking/hold')
  if (!check(held, { '선택 완료 200': (r) => r.status === 200 && json(r).unselectableSeatIds.length === 0 })) return abandon()

  const quantities = {}
  for (const seatId of mine) quantities[data.gradeBySeatId[seatId]] = (quantities[data.gradeBySeatId[seatId]] || 0) + 1
  const priced = post(
    '/booking/prices/validate',
    { quantities: Object.keys(quantities).map((grade) => ({ grade, quantity: quantities[grade] })) },
    run,
    'POST /booking/prices/validate',
  )
  if (!check(priced, { '가격 확인 204': (r) => r.status === 204 })) return abandon()
  get('/reservations/quote', run, 'GET /reservations/quote')

  // 모의 결제 서버가 1~3초 뒤 승인한다.
  const reserved = post('/reservations', null, Object.assign({ 'Idempotency-Key': `${__VU}-${__ITER}` }, run), 'POST /reservations')
  if (!check(reserved, { '결제와 확정 201': (r) => r.status === 201 })) return abandon()
  bookingDuration.add(Date.now() - startedAt)
  outcomes.success.add(1)
  get('/records', {}, 'GET /records')
}

function soldOut() {
  outcomes.soldOut.add(1)
}

function fail() {
  outcomes.failed.add(1)
  sleep(1)
}

// 중간에 실패하면 회차를 중단으로 끝내 자리를 비운다.
function abandon() {
  outcomes.failed.add(1)
  post('/performances/current/abandon', null, {}, 'POST /performances/current/abandon')
}

export function handleSummary(data) {
  const out = __ENV.SUMMARY_PATH
  const result = { stdout: summaryText(data) }
  if (out) result[out] = JSON.stringify(data, null, 1)
  return result
}

function summaryText(data) {
  const metric = (name) => data.metrics[name]
  const count = (name) => (metric(name) ? metric(name).values.count : 0)
  const lines = [`동시 연습 ${VUS}개, ${DURATION}, 난이도 ${DIFFICULTY}`]
  lines.push(`연습 결과: 성공 ${count('practice_success')}, 매진 ${count('practice_sold_out')}, 자리 없음 ${count('practice_rejected_full')}, 실패 ${count('practice_failed')}`)
  lines.push(`요청: ${count('http_reqs')}건 (${metric('http_reqs').values.rate.toFixed(1)}건/초), 실패율 ${(metric('http_req_failed').values.rate * 100).toFixed(2)}%`)
  const duration = metric('http_req_duration').values
  lines.push(`응답 시간(ms): 중간 ${duration.med.toFixed(0)}, p95 ${duration['p(95)'].toFixed(0)}, p99 ${duration['p(99)'].toFixed(0)}, 최대 ${duration.max.toFixed(0)}`)
  return lines.join('\n') + '\n'
}
