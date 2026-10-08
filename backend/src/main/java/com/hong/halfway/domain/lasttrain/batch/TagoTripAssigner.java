package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TAGO 시간표엔 열차번호가 없어서, "노선+방향+종착역" 그룹 안에서 출발시각 순서(순위)로 같은 물리적 열차를
 * 식별하는 합성 식별자를 만듦. 역마다 독립적으로 호출해도 같은 열차는 모든 정차역에서 같은 그룹·같은 순위로
 * 나타난다는 전제(해당 역간 급행 등 열차를 건너뛰는 운행 패턴이 없음)에 기반
 * 종착역 정보가 아예 없는 노선(예: GTX-A)은 방향까지만으로 묶음
 */
public final class TagoTripAssigner {

    private TagoTripAssigner() {
    }

    public record AssignedTrip(String depTime, String arrTime, String trainNo, String destStationName) {
    }

    public static List<AssignedTrip> assign(List<TagoTimetableResponse.Item> items) {
        Map<String, List<TagoTimetableResponse.Item>> grouped = items.stream()
                .collect(Collectors.groupingBy(TagoTripAssigner::groupKey, LinkedHashMap::new, Collectors.toList()));

        List<AssignedTrip> result = new ArrayList<>();
        for (Map.Entry<String, List<TagoTimetableResponse.Item>> entry : grouped.entrySet()) {
            List<TagoTimetableResponse.Item> ordered = entry.getValue().stream()
                    .sorted(Comparator.comparing(TagoTimetableResponse.Item::depTime))
                    .toList();
            for (int i = 0; i < ordered.size(); i++) {
                TagoTimetableResponse.Item item = ordered.get(i);
                String trainNo = entry.getKey() + ":" + (i + 1);
                String destStationName = item.endSubwayStationNm() != null ? item.endSubwayStationNm() : "TAGO";
                result.add(new AssignedTrip(item.depTime(), item.arrTime(), trainNo, destStationName));
            }
        }
        return result;
    }

    // 노선(subwayRouteId)+방향+종착역ID로 그룹을 묶음. 종착역 ID가 없으면 노선+방향까지만으로 묶음
    private static String groupKey(TagoTimetableResponse.Item item) {
        String end = item.endSubwayStationId() != null ? item.endSubwayStationId() : "X";
        return "T:" + item.subwayRouteId() + ":" + item.upDownTypeCode() + ":" + end;
    }
}
