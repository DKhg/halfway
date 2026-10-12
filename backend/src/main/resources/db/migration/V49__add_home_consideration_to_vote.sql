ALTER TABLE station_candidate ADD COLUMN consider_home BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE station_candidate_time ADD COLUMN departure_minutes SMALLINT;
ALTER TABLE station_candidate_time ADD COLUMN home_minutes SMALLINT;

COMMENT ON COLUMN station_candidate.consider_home IS '귀가역까지 고려해서 고정한 후보인지 (true면 station_candidate_time의 departure_minutes/home_minutes가 채워짐)';
COMMENT ON COLUMN station_candidate_time.departure_minutes IS '귀가역 고려 후보일 때, 출발역에서 이 후보까지 걸리는 시간(분). 고려 안 했으면 NULL';
COMMENT ON COLUMN station_candidate_time.home_minutes IS '귀가역 고려 후보일 때, 이 후보에서 귀가역까지 걸리는 시간(분). 고려 안 했으면 NULL';
