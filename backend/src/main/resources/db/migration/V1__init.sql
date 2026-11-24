-- 배포를 확정하기 전까지는 이 파일 하나만 고친다. 고친 뒤 로컬 DB는 `docker compose down -v`로 지우고 다시 만든다.

-- 사용자
CREATE TABLE users
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

-- 공연장
CREATE TABLE venues
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

-- 공연장 좌석: 공연장 안의 고정된 자리.
CREATE TABLE seats
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    venue_id   BIGINT      NOT NULL,
    row_no     INT         NOT NULL,
    column_no  INT         NOT NULL,
    grade      VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_seats_venue_position UNIQUE (venue_id, row_no, column_no),
    CONSTRAINT ck_seats_grade CHECK (grade IN ('VIP', 'R', 'S', 'A')),
    CONSTRAINT fk_seats_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
);

-- 공연
CREATE TABLE shows
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    venue_id   BIGINT       NOT NULL,
    title      VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_shows_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
);

-- 공연의 좌석 등급별 가격.
CREATE TABLE show_seat_prices
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    show_id    BIGINT      NOT NULL,
    grade      VARCHAR(10) NOT NULL,
    price      BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_show_seat_prices_show_grade UNIQUE (show_id, grade),
    CONSTRAINT ck_show_seat_prices_grade CHECK (grade IN ('VIP', 'R', 'S', 'A')),
    CONSTRAINT fk_show_seat_prices_show FOREIGN KEY (show_id) REFERENCES shows (id)
);

-- 공연 회차
CREATE TABLE performances
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    show_id    BIGINT      NOT NULL,
    start_at   DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_performances_show_start UNIQUE (show_id, start_at),
    CONSTRAINT fk_performances_show FOREIGN KEY (show_id) REFERENCES shows (id)
);

-- 공연 회차별 좌석: 한 회차에서 좌석 하나의 판매 상태
CREATE TABLE performance_seats
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    performance_id  BIGINT      NOT NULL,
    seat_id         BIGINT      NOT NULL,
    status          VARCHAR(10) NOT NULL,
    holder_id       BIGINT      NULL,
    hold_expires_at DATETIME(6) NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_performance_seats_performance_seat UNIQUE (performance_id, seat_id),
    CONSTRAINT uk_performance_seats_id_performance UNIQUE (id, performance_id),
    CONSTRAINT ck_performance_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'RESERVED')),
    CONSTRAINT ck_performance_seats_hold CHECK ((status = 'HELD' AND holder_id IS NOT NULL AND hold_expires_at IS NOT NULL) OR (status <> 'HELD' AND holder_id IS NULL AND hold_expires_at IS NULL)),
    KEY idx_performance_seats_holder (holder_id, performance_id),
    CONSTRAINT fk_performance_seats_performance FOREIGN KEY (performance_id) REFERENCES performances (id),
    CONSTRAINT fk_performance_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT fk_performance_seats_holder FOREIGN KEY (holder_id) REFERENCES users (id)
);

-- 예약.
CREATE TABLE reservations
(
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                    BIGINT      NOT NULL,
    performance_id             BIGINT      NOT NULL,
    status                     VARCHAR(10) NOT NULL,
    idempotency_key            VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    amount                     BIGINT      NOT NULL,
    payment_transaction_id     VARCHAR(64) NULL,
    confirmed_at               DATETIME(6) NULL,
    created_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reservations_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT uk_reservations_id_performance UNIQUE (id, performance_id),
    CONSTRAINT ck_reservations_status CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED')),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_performance FOREIGN KEY (performance_id) REFERENCES performances (id)
);

-- 예약 좌석: 예약이 확보한 공연 회차별 좌석과 예약 당시 가격.
-- 복합 FK로 예약의 회차와 공연 회차별 좌석의 회차가 같게 한다.
-- active는 예약이 확정된 동안만 TRUE이고 그 밖에는 NULL이다. UNIQUE는 NULL끼리 겹쳐도 막지 않으므로,
-- 확정된 예약 좌석만 공연 회차별 좌석마다 하나로 막는다(결제 대기 예약끼리 같은 좌석을 가지는 것은 정상).
CREATE TABLE reservation_seats
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    reservation_id      BIGINT      NOT NULL,
    performance_id      BIGINT      NOT NULL,
    performance_seat_id BIGINT      NOT NULL,
    price               BIGINT      NOT NULL,
    active              BOOLEAN     NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reservation_seats_reservation_performance_seat UNIQUE (reservation_id, performance_seat_id),
    CONSTRAINT uk_reservation_seats_active_performance_seat UNIQUE (performance_seat_id, active),
    CONSTRAINT ck_reservation_seats_active CHECK (active IS NULL OR active = TRUE),
    CONSTRAINT fk_reservation_seats_reservation FOREIGN KEY (reservation_id, performance_id) REFERENCES reservations (id, performance_id),
    CONSTRAINT fk_reservation_seats_performance_seat FOREIGN KEY (performance_seat_id, performance_id) REFERENCES performance_seats (id, performance_id)
);
