-- ARRIVETIME "00:00:00"도 LEFTTIME처럼 "값 없음"(시발역이라 도착기록이 없음) placeholder라
-- 지금까지는 둘 다 0분으로 잘못 저장돼 같은 역에서 시발/종착이 뒤바뀐 두 열차가 같은 train_no를 쓸 때 유니크 제약에 걸림
-- (예: 성수역 2호선 2017번 - 하나는 성수에서 출발, 다른 하나는 성수에서 종착, train_no가 같음)
-- arrive_minute을 NULL 허용으로 바꾸고, 유니크 키에 leave_minute도 포함해 두 유형을 구분
TRUNCATE TABLE train_timetable;

ALTER TABLE train_timetable ALTER COLUMN arrive_minute DROP NOT NULL;

DROP INDEX ux_train_timetable_station_train_week;
CREATE UNIQUE INDEX ux_train_timetable_station_train_week_time
    ON train_timetable (station_id, train_no, week_tag, arrive_minute, leave_minute);

COMMENT ON COLUMN train_timetable.arrive_minute IS '이 역 도착 시각(분). 이 역이 시발역이라 도착기록이 없으면 NULL. 자정 이후는 24시를 넘겨 표현 (00:02 -> 1442)';
