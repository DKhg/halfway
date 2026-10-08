ALTER TABLE room ADD COLUMN includes_food BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE room ADD COLUMN includes_cafe BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN room.includes_food IS '이 방에서 음식점 선택이 필요한지 여부 (FIXED 모드 등에서 음식점만/카페만 고를 때 사용)';
COMMENT ON COLUMN room.includes_cafe IS '이 방에서 카페 선택이 필요한지 여부 (FIXED 모드 등에서 음식점만/카페만 고를 때 사용)';
