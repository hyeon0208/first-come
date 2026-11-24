-- 테스트용 데이터. 공연장 1개(20행 × 20열), 공연 2개, 회차 5개. id를 고정해 테스트가 번호로 가리킨다.

INSERT INTO venues (id, name)
VALUES (1, '연습장 홀')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO shows (id, venue_id, title)
VALUES (1, 1, '모래시계 밴드 겨울 단독 공연'),
       (2, 1, '유리온실 3집 발매 기념 콘서트')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO show_seat_prices (id, show_id, grade, price)
VALUES (1, 1, 'VIP', 150000),
       (2, 1, 'R', 120000),
       (3, 1, 'S', 90000),
       (4, 1, 'A', 60000),
       (5, 2, 'VIP', 170000),
       (6, 2, 'R', 140000),
       (7, 2, 'S', 110000),
       (8, 2, 'A', 80000)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO performances (id, show_id, start_at)
VALUES (1, 1, '2026-12-25 10:00:00'),
       (2, 1, '2026-12-25 14:00:00'),
       (3, 1, '2026-12-25 19:00:00'),
       (4, 2, '2026-12-25 11:00:00'),
       (5, 2, '2026-12-25 17:00:00')
ON DUPLICATE KEY UPDATE id = id;

-- 등급은 행 범위(1~3 VIP, 4~8 R, 9~14 S, 15~20 A)다.
INSERT INTO seats (id, venue_id, row_no, column_no, grade)
SELECT *
FROM (SELECT (r.n - 1) * 20 + c.n AS id,
             1 AS venue_id,
             r.n AS row_no,
             c.n AS column_no,
             CASE WHEN r.n <= 3 THEN 'VIP' WHEN r.n <= 8 THEN 'R' WHEN r.n <= 14 THEN 'S' ELSE 'A' END AS grade
      FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
            UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
            UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
            UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) r
      CROSS JOIN
           (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
            UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
            UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
            UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) c) AS seat_rows
ON DUPLICATE KEY UPDATE seats.id = seats.id;

INSERT INTO performance_seats (id, performance_id, seat_id, status)
SELECT (p.id - 1) * 400 + s.id,
       p.id,
       s.id,
       'AVAILABLE'
FROM performances p
         JOIN shows sh ON sh.id = p.show_id
         JOIN seats s ON s.venue_id = sh.venue_id
WHERE p.id BETWEEN 1 AND 5
  AND s.id BETWEEN 1 AND 400
ON DUPLICATE KEY UPDATE performance_seats.id = performance_seats.id;
