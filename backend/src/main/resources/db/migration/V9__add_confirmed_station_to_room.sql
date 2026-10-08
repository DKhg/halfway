ALTER TABLE room ADD COLUMN confirmed_station_name VARCHAR(255);

COMMENT ON COLUMN room.confirmed_station_name IS '확정된 약속 장소 역 이름';
