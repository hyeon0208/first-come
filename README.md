# 선착순 연습장

콘서트 좌석 예매를 처음부터 끝까지 연습해 보는 사이트다. 닉네임만 넣으면 나만의 회차(좌석 1,008석)가 만들어지고, 서버가 움직이는 가상 경쟁자와 같은 좌석을 두고 겨룬다. 가상 경쟁자는 화면 연출이 아니라 사용자와 같은 경로로 좌석을 가져가므로, 이미 잡힌 좌석을 누르면 서버가 거절한다.

## 실행

```bash
docker compose up -d --build
```

- 화면: http://localhost:3000
- 서버: http://localhost:8080 (API 문서 http://localhost:8080/swagger-ui.html)
- 모의 결제 서버: localhost:8081, MySQL: localhost:3306 (둘 다 이 컴퓨터에서만 열린다)

스키마(`backend/src/main/resources/db/migration/V1__init.sql`)는 배포 전까지 한 파일을 고쳐 쓴다. 고친 뒤에는 로컬 DB를 지우고 다시 띄운다.

```bash
docker compose down -v
docker compose up -d --build
```

## 흐름

닉네임 입력 → 공연 목록 → 상세(난이도 선택 → 연습 시작 → 10초 카운트다운 → 예매하기) → 대기열 → 좌석 선택 → 가격 선택 → 예매 정보 입력 → 결제 → 예매 완료 → 내 예매

| 난이도 | 가상 경쟁자 | 매진 |
|---|---|---|
| 쉬움 | 200명이 5분에 걸쳐 | 매진되지 않음(절반쯤 팔림) |
| 보통 | 500명이 2분에 걸쳐 | 약 1분 40초 |
| 어려움 | 1,000명이 25초에 걸쳐 | 약 10초 |

## 예매 규칙

| 규칙 | 값 |
|---|---|
| 좌석 선택 시간 | 예매 화면에 들어온 순간부터 10분 |
| 가선점 | 좌석을 누르는 순간. 첫 좌석부터 7분이고 더 눌러도 늘지 않는다 |
| 선점(예매 가능 시간) | 선택 완료부터 7분. 가격 선택, 예매 정보 입력, 결제가 이 시간을 함께 쓴다 |
| 두 시간의 관계 | 좌석 선택 시간이 끝나도 선점 중이면 예매 가능 시간을 끝까지 준다 |
| 매수 | 한 회차에 4매 |
| 좌석 상태 | 들어올 때 한 번 받고, 그 뒤로는 새로고침을 눌러야 바뀐다 |
| 결제 뒤 확정 | 결제가 승인된 뒤 예매 가능 시간과 공연 시작 시각을 다시 확인한다. 어긋나면 확정하지 않고 결제를 자동 취소한다 |
| 결제 중복 | 같은 선점에는 결제가 한 번에 한 건만 진행된다. 진행 중에 온 요청은 멱등 키가 달라도 결제를 부르지 않고 거절한다 |

값은 `backend/src/main/resources/application.yml`의 `booking`, `competitor` 항목에 있다.

## 구조

```
backend/        Kotlin, Spring Boot, Spring Modulith, JPA, MySQL
payment-mock/   모의 결제 서버(별도 컨테이너). 1~3초 뒤 승인한다
frontend/       React, Vite, TypeScript
load/           부하 측정 스크립트와 결과
scripts/        확인 스크립트
docs/adr/       되돌리기 어려운 결정의 기록
GLOSSARY.md     용어
```

백엔드는 모듈로 나뉜다: `user`(닉네임과 세션), `show`(공연과 좌석 배치), `performance`(회차와 좌석 상태), `reservation`(예매와 결제), `competitor`(가상 경쟁자), `record`(연습 기록). 모듈은 `application` 패키지만 서로에게 공개하고, 모듈 사이에는 FK를 두지 않는다.

좌석 상태는 `선택 가능 → 가선점 → 선점 → 결제 중 → 예매 완료`로 바뀌고, 모든 전환은 "지금 이 상태이고, 이 참가자의 것이고, 만료되지 않았을 때만"을 조건으로 건 UPDATE 한 문장이다(`docs/adr/0001`). 결제는 트랜잭션 밖에서 부르고, 부르기 전에 좌석을 결제 중으로 잠근다(`docs/adr/0002`).

## 확인

| # | 무엇을 | 어떻게 |
|---|---|---|
| 1 | 닉네임부터 내 예매까지 한 번에 끝나고, 다시 시작하면 새 회차로 된다 | `npm --prefix frontend run e2e` (compose로 띄운 뒤), `BookingFlowApiTest` |
| 2 | 가상 경쟁자가 먼저 잡은 좌석은 서버가 거절한다 | `CompetitorTakesSeatTest`, e2e의 두 번째 테스트 |
| 3 | 결제 중 예매 가능 시간이 끝나거나 공연이 시작되면 확정하지 않고 결제를 취소한다 | `ReservationRecheckTest` |
| 4 | 같은 선점에 다른 멱등 키로 동시에 요청해도 결제 승인은 한 번이다 | `ReservationSinglePaymentConcurrencyTest` |
| 5 | 같은 좌석을 여럿이 노리면 한 명만 가져가고 교착이 없다(실제 MySQL) | `PerformanceSeatConcurrencyTest` |
| 6 | 모듈 경계 | `ModularityTest` |
| 7 | compose 한 번으로 네 서비스가 뜬다 | `scripts/check-compose.sh` |
| 8 | 동시 연습 수를 올려 가며 처리량, 응답 시간, 먼저 막히는 곳을 잰다 | `load/run-baseline.sh` (결과는 `load/results/`) |
| 9 | 이 서비스에 없는 규칙과 이름이 들어오지 않았다 | `scripts/check-no-traces.sh` |

난이도별 매진 시간은 `./gradlew :backend:test -Pslow`(`DifficultyCalibrationTest`)로 잰다.

```bash
./gradlew build                     # 백엔드와 모의 결제 서버의 테스트(Docker가 필요하다: Testcontainers MySQL)
npm --prefix frontend run check     # 화면의 lint, 테스트, 빌드
```
