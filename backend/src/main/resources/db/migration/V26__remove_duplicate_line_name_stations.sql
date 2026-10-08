-- TAGO 원본 데이터가 같은 물리 노선을 두 line 표기로 중복 등록해, LINE_ALIAS로 통일하기 전에 양쪽 표기 역이 이미 쌓여있었음
-- 두 표기 모두 자기 노선 내 운행엣지가 없어 환승역 외엔 고립됐던 문제. canonical 표기(LINE_ALIAS 타깃)만 남기고 비canonical 쪽 제거
DELETE FROM station_travel_time
WHERE from_station_id IN (
    SELECT id FROM station WHERE line IN ('인천선', '경춘선', '의정부경전철', '용인경전철', '경강선', '김포도시철도')
) OR to_station_id IN (
    SELECT id FROM station WHERE line IN ('인천선', '경춘선', '의정부경전철', '용인경전철', '경강선', '김포도시철도')
);

DELETE FROM station
WHERE line IN ('인천선', '경춘선', '의정부경전철', '용인경전철', '경강선', '김포도시철도');
