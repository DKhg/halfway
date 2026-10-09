-- TAGO "경의중앙" 데이터에 2014년 경의·중앙선 통합 전 옛 "경의선"(서울역 종착) 체계의 잔재로 보이는
-- 역이 섞여 있었음. tago_station_id가 현재 운행 중인 역(공덕=K4K312, 서강대=K4K313, 홍대입구=K4K314)과
-- 같은 번호대를 접두어만 다르게(K4P) 중복 사용(서울역=K4P313, 신촌=K4P314) — 현재 노선엔 없는 역으로 판단
-- 자기 노선 안에서 고립된 유령 노드였음
DELETE FROM train_timetable
WHERE station_id IN (
    SELECT id FROM station WHERE line = '경의중앙' AND name IN ('서울역', '신촌')
);

DELETE FROM station_travel_time
WHERE from_station_id IN (
    SELECT id FROM station WHERE line = '경의중앙' AND name IN ('서울역', '신촌')
) OR to_station_id IN (
    SELECT id FROM station WHERE line = '경의중앙' AND name IN ('서울역', '신촌')
);

DELETE FROM station
WHERE line = '경의중앙' AND name IN ('서울역', '신촌');
