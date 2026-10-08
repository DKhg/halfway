ALTER TABLE station ADD COLUMN fr_code VARCHAR(10);

COMMENT ON COLUMN station.fr_code IS '서울교통공사 내부 역코드. 열차 시간표 API(SearchSTNTimeTableByFRCodeService) 조회에 필요, 서울교통공사 운영 노선(1~9호선)만 채워짐';
