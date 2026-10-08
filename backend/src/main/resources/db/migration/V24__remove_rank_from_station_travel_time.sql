-- rank 2·3 사전 계산(V23)을 폐기하고, 막차 조회 시점에 rank=1 테이블을 오라클 삼아
-- 그때그때 대체 경로 후보를 찾는 방식으로 변경 (3시간+ 걸리는 전체쌍 사전계산 대신 요청 단위 계산)
-- 모든 행이 항상 유일한 경로 하나뿐이라 rank 컬럼 자체가 더 이상 필요 없음
DROP INDEX ux_station_travel_time_from_to_rank;
ALTER TABLE station_travel_time DROP COLUMN rank;
CREATE UNIQUE INDEX ux_station_travel_time_from_to ON station_travel_time (from_station_id, to_station_id);
