-- TAGO 원본 "서해선" 노선 데이터가 대곡 인근 경의중앙선 공용 구간 역까지 포함해서 줘서,
-- manual-lines.json 기준 서해선 범위(김포공항~원시)엔 없는 이 역들이 자기 노선 안에서 고립된 유령 노드였음
-- (같은 역이 "경의중앙" line으로 이미 정상 적재·연결돼 있어 중간지점 계산엔 영향 없었음)
DELETE FROM train_timetable
WHERE station_id IN (
    SELECT id FROM station WHERE line = '서해선' AND name IN ('일산', '풍산', '백마', '곡산', '대곡', '능곡')
);

DELETE FROM station_travel_time
WHERE from_station_id IN (
    SELECT id FROM station WHERE line = '서해선' AND name IN ('일산', '풍산', '백마', '곡산', '대곡', '능곡')
) OR to_station_id IN (
    SELECT id FROM station WHERE line = '서해선' AND name IN ('일산', '풍산', '백마', '곡산', '대곡', '능곡')
);

DELETE FROM station
WHERE line = '서해선' AND name IN ('일산', '풍산', '백마', '곡산', '대곡', '능곡');
