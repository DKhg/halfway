-- 서술형으로 끝나던 코멘트를 명사형으로 정리. predecessor_station_id는 rank 컬럼 삭제(V24)로 낡아진 설명도 같이 수정
COMMENT ON COLUMN station.fr_code IS '서울교통공사 내부 역코드. 열차 시간표 API(SearchSTNTimeTableByFRCodeService) 조회에 필요, 서울교통공사 운영 노선(1~9호선) 역만 보유';
COMMENT ON COLUMN station_travel_time.predecessor_station_id IS '도착역 바로 직전에 거친 역. 반복 조회로 출발역까지의 전체 경유역 순서 복원 가능';
COMMENT ON COLUMN train_timetable.train_no IS '열차 번호. 같은 열차의 다른 역 기록과 조인하면 실제 구간별 운행시간 파악 가능';
COMMENT ON COLUMN train_timetable.inout_tag IS '운행 방향 원본 코드. 노선마다 다른 의미 (상행/하행 또는 내선/외선)';
COMMENT ON COLUMN train_timetable.leave_minute IS '이 역 출발 시각(분). 종착역인 경우 NULL(출발기록 없음)';
