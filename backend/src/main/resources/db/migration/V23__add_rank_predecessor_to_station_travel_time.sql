-- 막차 계산은 최단경로 하나만으론 "최단경로의 막차가 끊겼지만 다른 환승로론 아직 갈 수 있는" 경우를 놓칠 수 있어
-- 역 쌍마다 서로 다른 경로 후보를 최대 3개(rank)까지 저장하고, 각자 자기 경로의 predecessor 체인을 가지게 함
DROP INDEX ux_station_travel_time_from_to;

ALTER TABLE station_travel_time ADD COLUMN rank SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE station_travel_time ADD COLUMN predecessor_station_id BIGINT REFERENCES station (id);

CREATE UNIQUE INDEX ux_station_travel_time_from_to_rank ON station_travel_time (from_station_id, to_station_id, rank);

COMMENT ON COLUMN station_travel_time.rank IS '같은 역 쌍의 경로 후보 순위 (1=최단, 2~3=서로 다른 대체 경로). 막차 역산에서 1순위 경로가 막히면 다음 순위로 재시도';
COMMENT ON COLUMN station_travel_time.predecessor_station_id IS '이 경로(rank)에서 도착역 바로 직전에 거친 역. 반복해서 따라가면 출발역까지의 전체 경유역 순서를 복원할 수 있음';
