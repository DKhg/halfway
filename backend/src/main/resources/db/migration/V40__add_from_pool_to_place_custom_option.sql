ALTER TABLE place_custom_option ADD COLUMN from_pool BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN place_custom_option.from_pool IS '다른 모임의 역 가게 풀(station_place_pool)에서 그대로 가져와 추가했는지 여부. 화면에 "다른 모임에서 추가"로 구분 표시';
