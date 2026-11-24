-- 배포를 확정하기 전까지는 이 파일 하나만 고친다. 고친 뒤 로컬 DB는 `docker compose down -v`로 지우고 다시 만든다.
-- 모듈(user, show, performance, reservation, record) 사이에는 FK를 두지 않는다. 같은 모듈 안에서만 둔다.

-- user ------------------------------------------------------------------------------------------

-- 사용자: 닉네임만으로 구분한다. 세션 쿠키 값은 해시로만 저장한다.
CREATE TABLE users
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    nickname           VARCHAR(12) NOT NULL,
    session_token_hash VARCHAR(64) NOT NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_session_token_hash UNIQUE (session_token_hash)
);

-- show ------------------------------------------------------------------------------------------

-- 공연장
CREATE TABLE venues
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

-- 공연장 좌석: 층, 구역, 열, 번호로 구분되는 고정된 자리. 등급은 구역 단위로 정해진다.
CREATE TABLE seats
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    venue_id   BIGINT      NOT NULL,
    floor      INT         NOT NULL,
    block      VARCHAR(1)  NOT NULL,
    row_no     INT         NOT NULL,
    seat_no    INT         NOT NULL,
    grade      VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_seats_position UNIQUE (venue_id, floor, block, row_no, seat_no),
    CONSTRAINT ck_seats_grade CHECK (grade IN ('R', 'S')),
    CONSTRAINT fk_seats_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
);

-- 공연
CREATE TABLE shows
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    venue_id        BIGINT       NOT NULL,
    title           VARCHAR(100) NOT NULL,
    running_minutes INT          NOT NULL,
    age_limit       VARCHAR(30)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_shows_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
);

-- 공연의 좌석 등급별 가격. name은 화면에 보이는 등급 이름이다(전석 공연은 두 등급의 이름과 가격이 같다).
CREATE TABLE show_seat_prices
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    show_id    BIGINT      NOT NULL,
    grade      VARCHAR(10) NOT NULL,
    name       VARCHAR(10) NOT NULL,
    price      BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_show_seat_prices_show_grade UNIQUE (show_id, grade),
    CONSTRAINT ck_show_seat_prices_grade CHECK (grade IN ('R', 'S')),
    CONSTRAINT fk_show_seat_prices_show FOREIGN KEY (show_id) REFERENCES shows (id)
);

-- performance -----------------------------------------------------------------------------------

-- 회차: 사용자가 연습을 시작할 때 그 사용자만을 위해 만들어지는 한 번의 판매.
-- active_user_id는 진행 중일 때만 사용자 id이고 끝나면 NULL이다. UNIQUE는 NULL끼리 겹쳐도 막지 않으므로
-- 한 사용자의 진행 중 회차만 하나로 막는다.
CREATE TABLE performances
(
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT      NOT NULL,
    show_id                   BIGINT      NOT NULL,
    difficulty                VARCHAR(10) NOT NULL,
    seed                      BIGINT      NOT NULL,
    status                    VARCHAR(10) NOT NULL,
    result                    VARCHAR(10) NULL,
    active_user_id            BIGINT      NULL,
    open_at                   DATETIME(6) NOT NULL,
    start_at                  DATETIME(6) NOT NULL,
    entered_at                DATETIME(6) NULL,
    seat_selection_expires_at DATETIME(6) NULL,
    ended_at                  DATETIME(6) NULL,
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_performances_active_user UNIQUE (active_user_id),
    CONSTRAINT ck_performances_difficulty CHECK (difficulty IN ('EASY', 'NORMAL', 'HARD')),
    CONSTRAINT ck_performances_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_performances_result CHECK (result IN ('SUCCESS', 'SOLD_OUT', 'TIMEOUT', 'ABANDONED')),
    CONSTRAINT ck_performances_ended CHECK ((status = 'ACTIVE' AND result IS NULL AND ended_at IS NULL AND active_user_id IS NOT NULL)
        OR (status = 'ENDED' AND result IS NOT NULL AND ended_at IS NOT NULL AND active_user_id IS NULL)),
    KEY idx_performances_status_ended (status, ended_at),
    KEY idx_performances_user (user_id)
);

-- 참가자: 한 회차에서 좌석을 두고 겨루는 주체. 0번이 사용자이고 1번부터 가상 경쟁자다.
-- 가선점 만료 시각은 첫 좌석을 누를 때, 선점 만료 시각은 선택 완료 때 정해진다.
CREATE TABLE performance_participants
(
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    performance_id       BIGINT      NOT NULL,
    participant_no       INT         NOT NULL,
    preselect_expires_at DATETIME(6) NULL,
    hold_expires_at      DATETIME(6) NULL,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_performance_participants_no UNIQUE (performance_id, participant_no),
    CONSTRAINT fk_performance_participants_performance FOREIGN KEY (performance_id) REFERENCES performances (id)
);

-- 회차 좌석: 한 회차에서 공연장 좌석 하나의 판매 상태.
-- AVAILABLE 선택 가능 → PRESELECTED 가선점 → HELD 선점 → PAYING 결제 중 → RESERVED 예매 완료
CREATE TABLE performance_seats
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    performance_id BIGINT      NOT NULL,
    seat_id        BIGINT      NOT NULL,
    status         VARCHAR(12) NOT NULL DEFAULT 'AVAILABLE',
    participant_no INT         NULL,
    expires_at     DATETIME(6) NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_performance_seats_seat UNIQUE (performance_id, seat_id),
    CONSTRAINT ck_performance_seats_status CHECK (status IN ('AVAILABLE', 'PRESELECTED', 'HELD', 'PAYING', 'RESERVED')),
    CONSTRAINT ck_performance_seats_owner CHECK ((status = 'AVAILABLE' AND participant_no IS NULL AND expires_at IS NULL)
        OR (status IN ('PRESELECTED', 'HELD', 'PAYING') AND participant_no IS NOT NULL AND expires_at IS NOT NULL)
        OR (status = 'RESERVED' AND participant_no IS NOT NULL AND expires_at IS NULL)),
    KEY idx_performance_seats_participant (performance_id, participant_no),
    CONSTRAINT fk_performance_seats_performance FOREIGN KEY (performance_id) REFERENCES performances (id)
);

-- reservation -----------------------------------------------------------------------------------

-- 예매: 참가자가 선점한 좌석 전체를 결제해 확보한 것. 결제 대기(PENDING)로 만들고 결제 뒤 확정하거나 실패로 바꾼다.
CREATE TABLE reservations
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    performance_id         BIGINT      NOT NULL,
    participant_no         INT         NOT NULL,
    status                 VARCHAR(10) NOT NULL,
    idempotency_key        VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    ticket_amount          BIGINT      NOT NULL,
    fee_amount             BIGINT      NOT NULL,
    payment_transaction_id VARCHAR(64) NULL,
    confirmed_at           DATETIME(6) NULL,
    created_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reservations_idempotency_key UNIQUE (performance_id, participant_no, idempotency_key),
    CONSTRAINT ck_reservations_status CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED'))
);

-- 예매 좌석: 예매가 확보한 좌석과 예매 당시 가격.
-- active는 예매가 확정된 동안만 TRUE이고 그 밖에는 NULL이다. 한 좌석이 두 번 확정되는 것을 UNIQUE로 막는다.
CREATE TABLE reservation_seats
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT      NOT NULL,
    performance_id BIGINT      NOT NULL,
    seat_id        BIGINT      NOT NULL,
    price          BIGINT      NOT NULL,
    active         BOOLEAN     NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reservation_seats_reservation_seat UNIQUE (reservation_id, seat_id),
    CONSTRAINT uk_reservation_seats_active_seat UNIQUE (performance_id, seat_id, active),
    CONSTRAINT ck_reservation_seats_active CHECK (active IS NULL OR active = TRUE),
    CONSTRAINT fk_reservation_seats_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id)
);

-- record ----------------------------------------------------------------------------------------

-- 연습 기록: 회차가 끝난 뒤 남는 결과. 회차와 좌석, 예매 데이터가 지워져도 남는다.
CREATE TABLE records
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    performance_id  BIGINT       NOT NULL,
    show_title      VARCHAR(100) NOT NULL,
    difficulty      VARCHAR(10)  NOT NULL,
    result          VARCHAR(10)  NOT NULL,
    elapsed_seconds BIGINT       NOT NULL,
    ended_at        DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_records_performance UNIQUE (performance_id),
    KEY idx_records_user (user_id, id)
);

-- 고정 데이터 ------------------------------------------------------------------------------------

INSERT INTO venues (id, name)
VALUES (1, '연습장 홀');

-- 1층 A·B·C·D 구역은 12열 × 16번(192석씩), 2층 A·B·C 구역은 4열 × 20번(80석씩). 합계 1,008석.
-- R석은 무대 쪽인 1층 A·B, S석은 1층 C·D와 2층 전체다.
INSERT INTO seats (venue_id, floor, block, row_no, seat_no, grade)
WITH RECURSIVE numbers AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM numbers WHERE n < 20),
               blocks AS (SELECT 1 AS floor, 'A' AS block, 12 AS row_count, 16 AS seat_count, 'R' AS grade
                          UNION ALL SELECT 1, 'B', 12, 16, 'R'
                          UNION ALL SELECT 1, 'C', 12, 16, 'S'
                          UNION ALL SELECT 1, 'D', 12, 16, 'S'
                          UNION ALL SELECT 2, 'A', 4, 20, 'S'
                          UNION ALL SELECT 2, 'B', 4, 20, 'S'
                          UNION ALL SELECT 2, 'C', 4, 20, 'S')
SELECT 1, b.floor, b.block, r.n, s.n, b.grade
FROM blocks b
         JOIN numbers r ON r.n <= b.row_count
         JOIN numbers s ON s.n <= b.seat_count
ORDER BY b.floor, b.block, r.n, s.n;

INSERT INTO shows (id, venue_id, title, running_minutes, age_limit)
VALUES (1, 1, '모래시계 밴드 겨울 단독 공연 〈첫눈〉', 120, '만 7세 이상'),
       (2, 1, '유리온실 3집 발매 기념 콘서트', 110, '만 7세 이상'),
       (3, 1, '새벽 네 시 라디오 공개방송', 90, '전체 관람가');

INSERT INTO show_seat_prices (show_id, grade, name, price)
VALUES (1, 'R', 'R석', 132000),
       (1, 'S', 'S석', 99000),
       (2, 'R', 'R석', 121000),
       (2, 'S', 'S석', 88000),
       (3, 'R', '전석', 55000),
       (3, 'S', '전석', 55000);
