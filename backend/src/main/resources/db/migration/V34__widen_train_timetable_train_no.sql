-- TAGO 노선은 실제 열차번호가 없어 "방향+종착역+시간순위"로 만든 합성 식별자를 train_no에 대신 저장함.
-- 서울교통공사 숫자 열차번호보다 길어서 컬럼 폭을 넓힘 (저장되는 값·조회 로직은 기존과 동일)
ALTER TABLE train_timetable ALTER COLUMN train_no TYPE VARCHAR(40);

COMMENT ON COLUMN train_timetable.train_no IS '열차 번호(서울교통공사) 또는 합성 식별자(TAGO 노선: 방향+종착역+시간순위). 같은 값의 다른 역 기록과 조인하면 실제 구간별 운행시간을 알 수 있음';
