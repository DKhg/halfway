package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse;
import com.hong.halfway.domain.station.entity.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TAGO 시간표엔 열차번호가 없어서, 노선을 물리적 순서대로 따라가며 "노선+방향+종착역" 그룹 안에서
 * 직전 역이 이미 배정한 열차 중 가장 가까운 시간의 것과 이어붙여 같은 물리적 열차를 식별하는
 * 합성 식별자를 만듦. 역마다 완전히 독립적으로 순위를 매기면, 중간역에서 시종착하는 열차가 있을 때
 * (예: 경의중앙선 왕십리~청량리 구간) 역마다 그룹 인원수가 달라져서 같은 순위가 실제로는 다른
 * 물리적 열차를 가리키게 되므로, 노선 순서를 따라가며 체이닝하는 방식으로 바꿈
 * 종착역 정보가 아예 없는 노선(예: GTX-A)은 방향까지만으로 묶음
 */
public final class TagoTripAssigner {

    // 물리적으로 인접한 역 사이 운행시간 상한 추정치. 직전 역 열차 중 이보다 더 이른 것은 중간에
    // 종착해서 이 역까지 안 온 것으로 보고 추적을 그만둠 (너무 크면 전혀 다른 열차를 잘못 이어붙이게 됨)
    private static final int MAX_HOP_MINUTES = 20;

    private TagoTripAssigner() {
    }

    public record AssignedTrip(String depTime, String arrTime, String trainNo, String destStationName) {
    }

    // 한 역의 원본 TAGO 응답. orderedStationItems는 노선 물리적 순서대로 전달해야 함
    public record StationItems(Station station, List<TagoTimetableResponse.Item> items) {
    }

    // 그룹(노선+방향+종착역)별로 체인을 이어가기 위한 상태: 직전 역에서 배정한 (시각,번호) 목록과 다음 발급 번호
    private record TimedSeq(int timeMinutes, int seq) {
    }

    private static final class ChainState {
        private List<TimedSeq> previousAssigned = List.of();
        private int nextSeq = 1;
    }

    /**
     * 노선 전체(물리적 순서대로 정렬된 역별 원본 시간표)를 받아 역마다 합성 식별자를 매김
     */
    public static Map<Station, List<AssignedTrip>> assignAlongLine(List<StationItems> orderedStationItems) {
        Map<String, ChainState> chainByGroup = new HashMap<>();
        Map<Station, List<AssignedTrip>> result = new LinkedHashMap<>();

        for (StationItems stationItems : orderedStationItems) {
            Map<String, List<TagoTimetableResponse.Item>> grouped = stationItems.items().stream()
                    .collect(Collectors.groupingBy(TagoTripAssigner::groupKey, LinkedHashMap::new, Collectors.toList()));

            List<AssignedTrip> stationResult = new ArrayList<>();
            for (Map.Entry<String, List<TagoTimetableResponse.Item>> entry : grouped.entrySet()) {
                String groupKey = entry.getKey();
                List<TagoTimetableResponse.Item> ordered = entry.getValue().stream()
                        .sorted(Comparator.comparing(TagoTripAssigner::representativeTime))
                        .toList();

                ChainState state = chainByGroup.computeIfAbsent(groupKey, k -> new ChainState());
                List<TimedSeq> previous = state.previousAssigned;
                List<TimedSeq> currentAssigned = new ArrayList<>();
                // previous에서 "아직 확정 안 된(소비도 포기도 안 된)" 다음 후보의 인덱스 - 1
                int prevIndex = -1;

                for (TagoTimetableResponse.Item item : ordered) {
                    int time = parseTimeMinutes(representativeTime(item));
                    // 이 역 시각보다 MAX_HOP_MINUTES 넘게 이른 직전 역 열차는 중간에 종착한 것으로 보고 포기
                    while (prevIndex + 1 < previous.size() && previous.get(prevIndex + 1).timeMinutes() < time - MAX_HOP_MINUTES) {
                        prevIndex++;
                    }
                    int seq;
                    if (prevIndex + 1 < previous.size() && previous.get(prevIndex + 1).timeMinutes() <= time) {
                        prevIndex++;
                        seq = previous.get(prevIndex).seq();
                    } else {
                        // 직전 역에 이어받을 열차가 없음 - 이 역에서 새로 시작하는 열차로 보고 새 번호 발급
                        seq = state.nextSeq++;
                    }
                    currentAssigned.add(new TimedSeq(time, seq));

                    String trainNo = groupKey + ":" + seq;
                    String destStationName = item.endSubwayStationNm() != null ? item.endSubwayStationNm() : "TAGO";
                    stationResult.add(new AssignedTrip(item.depTime(), item.arrTime(), trainNo, destStationName));
                }

                state.previousAssigned = currentAssigned;
            }

            result.put(stationItems.station(), stationResult);
        }
        return result;
    }

    // 노선(subwayRouteId)+방향+종착역ID로 그룹을 묶음. 종착역 ID가 없으면 노선+방향까지만으로 묶음
    private static String groupKey(TagoTimetableResponse.Item item) {
        String end = item.endSubwayStationId() != null ? item.endSubwayStationId() : "X";
        return "T:" + item.subwayRouteId() + ":" + item.upDownTypeCode() + ":" + end;
    }

    // depTime이 없거나 "0"(이 방향 종착역이라 출발기록이 없다는 placeholder)이면 arrTime을 대신 씀.
    // 둘 다 없는 경우는 실질적으로 없다고 보고 끝 시각으로 처리(정렬·비교에서 null 방지)
    private static String representativeTime(TagoTimetableResponse.Item item) {
        if (isValidTime(item.depTime())) {
            return item.depTime();
        }
        return isValidTime(item.arrTime()) ? item.arrTime() : "235959";
    }

    private static boolean isValidTime(String hhmmss) {
        return hhmmss != null && hhmmss.length() >= 4 && !"0".equals(hhmmss);
    }

    // "061900" -> 379(분). representativeTime()이 항상 유효한 형식을 보장하므로 별도 검증 없이 파싱
    private static int parseTimeMinutes(String hhmmss) {
        int hour = Integer.parseInt(hhmmss.substring(0, 2));
        int minute = Integer.parseInt(hhmmss.substring(2, 4));
        return hour * 60 + minute;
    }
}
