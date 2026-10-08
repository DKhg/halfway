ALTER TABLE room ADD COLUMN confirmed_place_name VARCHAR(255);

COMMENT ON COLUMN room.confirmed_place_name IS '확정된 약속 가게 이름';
