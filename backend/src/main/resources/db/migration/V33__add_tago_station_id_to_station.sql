-- TAGO(국토교통부) 지하철정보 API의 역 ID. 서울교통공사 외 노선(신분당선, 수인분당선, GTX-A 등)의
-- 막차 시간표를 TAGO에서 조회할 때 사용. fr_code와 역할은 같고 적용 대상 노선만 다름
ALTER TABLE station ADD COLUMN tago_station_id VARCHAR(20);

COMMENT ON COLUMN station.tago_station_id IS 'TAGO 지하철역 ID. 열차 시간표 API(GetSubwaySttnAcctoSchdulList) 조회에 필요, 서울교통공사 외 노선 역만 보유';
