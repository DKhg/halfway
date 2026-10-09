ALTER TABLE users ADD COLUMN home_station_name VARCHAR(255);

COMMENT ON COLUMN users.home_station_name IS '귀가역. 방마다 따로가 아니라 계정(익명 ID) 전체에 하나, 막차 계산 모달에 미리 채워주는 용도';
