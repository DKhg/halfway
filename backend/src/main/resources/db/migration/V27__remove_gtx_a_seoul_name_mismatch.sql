-- GTX-A manual-lines.json이 "서울역"을 "서울"로 잘못 표기해서, 같은 역인데 1호선 등 다른 노선의 "서울역"과 이름이 안 맞아 환승이 전혀 안 생기던 문제
-- "운정중앙-킨텍스-서울-동탄" 4역 체인이 "서울역"을 포함한 나머지 GTX-A 역(대곡, 연신내, 수서, 성남, 구성)과도 단절돼 있었음
-- manual-lines.json을 9역 전체 체인(운정중앙~동탄, "서울역" 표기로 통일)으로 교체했으니, 더는 안 쓰는 "서울" 표기 row 제거
-- "서울"이 운정중앙-동탄 경로의 경유역(predecessor)으로도 참조돼 있어 from/to만 지우면 FK 위반 발생
-- station_travel_time은 배치가 매번 통째로 재생성하므로 여기서 전체 삭제해도 안전함
DELETE FROM station_travel_time;

DELETE FROM station WHERE line = 'GTX-A' AND name = '서울';
