ALTER TABLE station_place_pool RENAME COLUMN added_by_user_id TO user_id;

COMMENT ON COLUMN station_place_pool.user_id IS '이 가게를 역 풀에 최초로 적재한 사용자 (정보 표시용, 삭제 권한과는 무관)';
