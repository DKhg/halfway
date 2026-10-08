package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.batch.dto.ManualLineOrder;
import com.hong.halfway.domain.station.batch.dto.SeoulStationTimeResponse;
import com.hong.halfway.domain.station.entity.Station;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 역(노선 단위 노드) 그래프 구성. 운행 엣지(서울 열린데이터광장 + 수동 노선 순서) + 환승 엣지(같은 역, 다른 노선)
 */
@Slf4j
@Component
public class StationGraphBuilder {

    // 역간 소요시간을 알 수 없을 때 쓰는 평균값 (서울 열린데이터광장 실측 평균)
    private static final double AVG_SPEED_KM_PER_HOUR = 42.5;
    private static final int DEFAULT_SEGMENT_MINUTES = 2;

    // 실제 환승 데이터가 없을 때만 쓰는 기본값 (docs/DOMAIN.md: 도보 4분 + 대기 3분)
    private static final int DEFAULT_TRANSFER_MINUTES = 7;
    private static final String TRANSFER_TIMES_RESOURCE = "station-order/transfer-times.csv";

    // 역당 정차시간 (모든 역 동일하게 근사)
    private static final int DWELL_SECONDS = 30;

    // 서울교통공사_환승역거리 소요시간 정보(OA-13290)는 "공항철도"처럼 접미사 붙은 이름을 쓰는 경우가 있어 매핑
    private static final Map<String, String> TRANSFER_LINE_ALIAS = Map.of(
            "공항철도", "공항",
            "경의중앙선", "경의중앙",
            "수인분당선", "수인분당",
            "신분당선", "신분당",
            "경춘선", "경춘");

    // 서울 열린데이터광장 역간 소요시간 원본에 역명이 다르게 들어간 경우 (예: "신내역" -> "신내")
    // 역명 개정 후에도 역간 소요시간 API는 구 역명을 그대로 주는 경우가 있어 추가 매핑 필요
    // (당고개->불암산 2025년경, 뚝섬유원지->자양 2024-02-29 개정)
    private static final Map<String, String> SEOUL_STATION_NAME_ALIAS = Map.of(
            "신내역", "신내",
            "당고개", "불암산",
            "뚝섬유원지", "자양");

    // 서울 열린데이터광장 역간 소요시간 API는 지선이 있는 노선에서 분기 구조를 반영 안 하고 한 줄로 나열해서 줌
    // (예: 5호선 응답이 ...하남시청,하남검단산(하남지선 끝),둔촌동(마천지선 시작)... 순서로 와서, 실제로는 강동을
    // 거쳐야 하는데 "바로 다음 행"이라는 이유로 가짜 지름길 엣지가 생김 - 키 형식은 "노선:이전역:다음역"
    private static final Set<String> BRANCH_BOUNDARY_SKIP = Set.of(
            "5호선:하남검단산:둔촌동", // 하남지선 끝 -> 마천지선 시작 (실제로는 강동 경유)
            "2호선:시청:용답", // 순환선 완주 -> 성수지선 시작 (실제로는 성수에서 분기)
            "2호선:신설동:도림천"); // 성수지선 끝 -> 신정지선 시작 (실제로는 신도림에서 분기)

    /**
     * 전체 역 그래프 생성 (운행 엣지 + 환승 엣지). 다익스트라 계산 입력으로 사용
     */
    public Map<Long, List<Edge>> build(List<Station> allStations, List<SeoulStationTimeResponse.Row> seoulRows,
            List<ManualLineOrder> manualLines) {
        // TAGO는 "양재(서초구청)"처럼 부기역명을 붙여서 주는 경우가 많아 기본 역명 기준으로 매칭
        Map<String, Station> byNameAndLine = new HashMap<>();
        for (Station station : allStations) {
            byNameAndLine.put(key(baseName(station.getName()), station.getLine()), station);
        }

        Map<Long, List<Edge>> graph = new HashMap<>();
        for (Station station : allStations) {
            graph.put(station.getId(), new ArrayList<>());
        }

        addSeoulEdges(graph, byNameAndLine, seoulRows);
        addManualEdges(graph, byNameAndLine, manualLines);
        addTransferEdges(graph, allStations);
        return graph;
    }

    // 서울 열린데이터광장 인접역 운행시간을 노선별로 묶어 순서대로 엣지 생성
    private void addSeoulEdges(Map<Long, List<Edge>> graph, Map<String, Station> byNameAndLine,
            List<SeoulStationTimeResponse.Row> rows) {
        Map<String, List<SeoulStationTimeResponse.Row>> byLine = new LinkedHashMap<>();
        for (SeoulStationTimeResponse.Row row : rows) {
            byLine.computeIfAbsent(normalizeLine(row.line()), k -> new ArrayList<>()).add(row);
        }

        int added = 0;
        for (List<SeoulStationTimeResponse.Row> lineRows : byLine.values()) {
            for (int i = 1; i < lineRows.size(); i++) {
                SeoulStationTimeResponse.Row prev = lineRows.get(i - 1);
                SeoulStationTimeResponse.Row curr = lineRows.get(i);
                String boundaryKey = normalizeLine(curr.line()) + ":" + prev.stationName() + ":" + curr.stationName();
                if (BRANCH_BOUNDARY_SKIP.contains(boundaryKey)) {
                    continue;
                }
                // "자양(뚝섬한강공원)"처럼 부기역명이 붙거나 "미아사거리 "처럼 끝에 공백이 붙은 행이 있어 baseName으로 맞춰야 매칭됨
                Station from = byNameAndLine.get(key(normalizeSeoulStationName(prev.stationName()), normalizeLine(prev.line())));
                Station to = byNameAndLine.get(key(normalizeSeoulStationName(curr.stationName()), normalizeLine(curr.line())));
                if (from == null || to == null) {
                    log.error("[에러] 서울 열린데이터광장 역을 찾을 수 없음 - {} -> {}", prev.stationName(), curr.stationName());
                    continue;
                }
                int runningSeconds = parseHmToSeconds(curr.hm());
                addBidirectionalEdge(graph, from.getId(), to.getId(), runningSeconds, false);
                added++;
            }
        }
        log.info("[완료] 서울 열린데이터광장 운행 엣지 생성 - {}건", added);
    }

    // 수동 입력 노선 순서(manual-lines.json)로 엣지 생성. 실측 분(minutes) 있으면 그걸 쓰고, 없으면 거리를 평균속도로 환산, 둘 다 없으면 기본값
    private void addManualEdges(Map<Long, List<Edge>> graph, Map<String, Station> byNameAndLine,
            List<ManualLineOrder> manualLines) {
        int added = 0;
        for (ManualLineOrder lineOrder : manualLines) {
            List<ManualLineOrder.StationEntry> stations = lineOrder.stations();
            for (int i = 1; i < stations.size(); i++) {
                Station from = byNameAndLine.get(key(stations.get(i - 1).name(), lineOrder.line()));
                Station to = byNameAndLine.get(key(stations.get(i).name(), lineOrder.line()));
                if (from == null || to == null) {
                    log.error("[에러] 수동 노선 역을 찾을 수 없음 - line: {}", lineOrder.line());
                    continue;
                }
                ManualLineOrder.StationEntry entry = stations.get(i);
                int minutes = segmentMinutes(entry);
                int runningSeconds = Math.max(minutes, 1) * 60;
                addBidirectionalEdge(graph, from.getId(), to.getId(), runningSeconds, false);
                added++;
            }
        }
        log.info("[완료] 수동 입력 운행 엣지 생성 - {}건", added);
    }

    // GTX-A처럼 지하철 평균속도로 환산하면 안 맞는 노선은 실측 분을 직접 넣어두므로 그게 있으면 최우선 사용
    private int segmentMinutes(ManualLineOrder.StationEntry entry) {
        if (entry.minutes() != null) {
            return entry.minutes();
        }
        if (entry.distanceKm() != null) {
            return (int) Math.round(entry.distanceKm() / AVG_SPEED_KM_PER_HOUR * 60);
        }
        return DEFAULT_SEGMENT_MINUTES;
    }

    // 같은 역 이름의 서로 다른 노선끼리 환승 엣지 생성. 실측 환승시간 있으면 그걸 쓰고 없으면 기본값
    private void addTransferEdges(Map<Long, List<Edge>> graph, List<Station> allStations) {
        Map<String, Integer> transferSecondsByKey = loadTransferTimes();

        Map<String, List<Station>> byName = new HashMap<>();
        for (Station station : allStations) {
            byName.computeIfAbsent(baseName(station.getName()), k -> new ArrayList<>()).add(station);
        }

        int added = 0;
        int realDataUsed = 0;
        for (List<Station> sameName : byName.values()) {
            for (int i = 0; i < sameName.size(); i++) {
                for (int j = i + 1; j < sameName.size(); j++) {
                    Station a = sameName.get(i);
                    Station b = sameName.get(j);
                    Integer seconds = transferSecondsByKey.get(transferKey(baseName(a.getName()), a.getLine(), b.getLine()));
                    if (seconds == null) {
                        seconds = transferSecondsByKey.get(transferKey(baseName(b.getName()), b.getLine(), a.getLine()));
                    }
                    if (seconds != null) {
                        realDataUsed++;
                    } else {
                        seconds = DEFAULT_TRANSFER_MINUTES * 60;
                    }
                    // 환승 엣지는 이미 도보+대기 시간이라 정차시간을 따로 더하지 않음
                    addBidirectionalEdge(graph, a.getId(), b.getId(), seconds, true);
                    added++;
                }
            }
        }
        log.info("[완료] 환승 엣지 생성 - {}건 (실측 {}건, 기본값 {}건)", added, realDataUsed, added - realDataUsed);
    }

    // 서울교통공사_환승역거리 소요시간 정보(OA-13290) CSV를 (역명|출발노선|도착노선) -> 초 맵으로 파싱
    private Map<String, Integer> loadTransferTimes() {
        Map<String, Integer> result = new HashMap<>();
        try (InputStream is = new ClassPathResource(TRANSFER_TIMES_RESOURCE).getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // 헤더 건너뜀
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 6) {
                    continue;
                }
                String fromLine = normalizeLine(cols[1].trim());
                String stationName = cols[2].trim();
                String toLine = normalizeTransferLine(cols[3].trim());
                int seconds = parseHmToSeconds(cols[5].trim());
                result.put(transferKey(stationName, fromLine, toLine), seconds);
            }
        } catch (IOException e) {
            log.error("[에러] 환승시간 데이터 로드 실패", e);
        }
        return result;
    }

    // 환승시간 맵 조회용 키
    private String transferKey(String name, String fromLine, String toLine) {
        return name + "|" + fromLine + "|" + toLine;
    }

    // "4호선"류는 그대로 두고, "공항철도"처럼 매핑이 필요한 것만 우리 station.line 표기로 변환
    private String normalizeTransferLine(String line) {
        return TRANSFER_LINE_ALIAS.getOrDefault(line, line);
    }

    // 부기역명 제거 + 공백 정리 + "신내역"처럼 다르게 들어간 이름 매핑
    private String normalizeSeoulStationName(String name) {
        String base = baseName(name);
        return SEOUL_STATION_NAME_ALIAS.getOrDefault(base, base);
    }

    // 지하철은 양방향 운행이라 엣지를 양쪽에 다 추가. 운행 엣지는 도착역 정차시간을 더함 (환승 엣지는 이미 도보+대기 포함이라 제외)
    private void addBidirectionalEdge(Map<Long, List<Edge>> graph, Long a, Long b, int runningSeconds, boolean transfer) {
        int dwell = transfer ? 0 : DWELL_SECONDS;
        graph.get(a).add(new Edge(b, runningSeconds + dwell, transfer));
        graph.get(b).add(new Edge(a, runningSeconds + dwell, transfer));
    }

    // "1" -> "1호선", 이미 "1호선" 형태면 그대로
    private String normalizeLine(String line) {
        return line.endsWith("호선") ? line : line + "호선";
    }

    // 역 조회용 키
    private String key(String name, String line) {
        return name + "|" + line;
    }

    // "양재(서초구청)" -> "양재" (부기역명 제거), "미아사거리 "처럼 끝공백만 있는 경우도 정리
    private String baseName(String name) {
        int parenIndex = name.indexOf('(');
        return (parenIndex >= 0 ? name.substring(0, parenIndex) : name).trim();
    }

    // "2:13" -> 133초 (분 단위로 먼저 반올림하면 정차시간 합산 때 오차가 누적돼서 초 단위 그대로 유지)
    private int parseHmToSeconds(String hm) {
        String[] parts = hm.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        return minutes * 60 + seconds;
    }
}
