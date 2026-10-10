package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.entity.Station;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * GTFS(국가교통DB 표준) stop_times.txt에서 "같은 trip_id 안의 연속한 두 정차역" 사이 실제 경과시간을 뽑아
 * 역간 운행 엣지로 씀. 급행 트립은 역을 건너뛰어 인접역 소요시간이 아니라서 제외
 * (1~8호선처럼 서울 열린데이터광장 실측값이 있는 노선 외의, 지금까지 거리 환산·기본값으로만 채워져 있던
 * 노선들의 정확도를 끌어올리기 위함 - 9호선은 시간표는 서울교통공사 데이터를 쓰지만 역간 소요시간 그래프는
 * 지금까지 아예 실측 데이터가 없었어서 이걸로 보강함)
 */
@Slf4j
@Component
public class GtfsAdjacentTimeLoader {

    public record RunningTime(Long fromStationId, Long toStationId, int seconds) {
    }

    public record Result(List<RunningTime> runningTimes, Set<String> coveredLines) {
    }

    /**
     * GTFS 폴더에서 대상 노선(급행 제외) 트립들의 인접역 소요시간을 계산. 각 역쌍은 여러 트립의 중간값(median)을 씀
     */
    public Result load(Path gtfsDir, List<Station> allStations) {
        Map<String, GtfsLineResolver.RouteInfo> routeInfoByRouteId = loadRoutes(gtfsDir.resolve("routes.txt"));
        if (routeInfoByRouteId.isEmpty()) {
            return new Result(List.of(), Set.of());
        }
        Map<String, GtfsLineResolver.RouteInfo> routeInfoByTripId = loadTrips(gtfsDir.resolve("trips.txt"), routeInfoByRouteId);
        Map<String, String> baseNameByStopId = loadStopBaseNames(gtfsDir.resolve("stops.txt"));

        Map<String, Station> stationByNameAndLine = new HashMap<>();
        for (Station station : allStations) {
            stationByNameAndLine.put(station.getName() + "|" + station.getLine(), station);
        }

        Map<String, List<Integer>> secondsByPairKey = new HashMap<>();
        Set<String> coveredLines = new HashSet<>();
        streamStopTimesByTrip(gtfsDir.resolve("stop_times.txt"), rows -> {
            String tripId = rows.get(0)[0];
            GtfsLineResolver.RouteInfo info = routeInfoByTripId.get(tripId);
            if (info == null || info.express()) {
                return;
            }
            List<String[]> ordered = rows.stream()
                    .sorted(Comparator.comparingInt(r -> Integer.parseInt(r[4])))
                    .toList();
            for (int i = 1; i < ordered.size(); i++) {
                String prevStopId = ordered.get(i - 1)[3];
                String currStopId = ordered.get(i)[3];
                String prevBase = baseNameByStopId.get(prevStopId);
                String currBase = baseNameByStopId.get(currStopId);
                if (prevBase == null || currBase == null) {
                    continue;
                }
                Station from = stationByNameAndLine.get(prevBase + "|" + info.line());
                Station to = stationByNameAndLine.get(currBase + "|" + info.line());
                if (from == null || to == null) {
                    continue;
                }
                int prevDeparture = parseSeconds(ordered.get(i - 1)[2]);
                int currArrival = parseSeconds(ordered.get(i)[1]);
                if (prevDeparture < 0 || currArrival < 0 || currArrival <= prevDeparture) {
                    continue;
                }
                String pairKey = from.getId() < to.getId() ? from.getId() + ":" + to.getId() : to.getId() + ":" + from.getId();
                secondsByPairKey.computeIfAbsent(pairKey, k -> new ArrayList<>()).add(currArrival - prevDeparture);
                coveredLines.add(info.line());
            }
        });

        List<RunningTime> result = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : secondsByPairKey.entrySet()) {
            List<Integer> sorted = entry.getValue();
            Collections.sort(sorted);
            int median = sorted.get(sorted.size() / 2);
            String[] ids = entry.getKey().split(":");
            result.add(new RunningTime(Long.parseLong(ids[0]), Long.parseLong(ids[1]), median));
        }
        log.info("[완료] GTFS 역간 소요시간 계산 - {}개 노선, {}개 역쌍", coveredLines.size(), result.size());
        return new Result(result, coveredLines);
    }

    // routes.txt에서 대상 노선(route_long_name이 "<상행>"/"<하행>"으로 끝나는 지하철/경전철 노선)만 추출
    private Map<String, GtfsLineResolver.RouteInfo> loadRoutes(Path routesFile) {
        Map<String, GtfsLineResolver.RouteInfo> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(routesFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 5) {
                    continue;
                }
                GtfsLineResolver.resolve(cols[3]).ifPresent(info -> result.put(cols[0], info));
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS routes.txt 로드 실패", e);
        }
        return result;
    }

    // trips.txt에서 대상 route_id에 속하는 trip_id만 추려 RouteInfo와 연결
    private Map<String, GtfsLineResolver.RouteInfo> loadTrips(Path tripsFile, Map<String, GtfsLineResolver.RouteInfo> routeInfoByRouteId) {
        Map<String, GtfsLineResolver.RouteInfo> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(tripsFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 3) {
                    continue;
                }
                GtfsLineResolver.RouteInfo info = routeInfoByRouteId.get(cols[0]);
                if (info != null) {
                    result.put(cols[2], info);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS trips.txt 로드 실패", e);
        }
        return result;
    }

    // stops.txt에서 철도 정류장(RS_ 접두사)의 역 기본 이름만 추출 ("왕십리(경의중앙선)" -> "왕십리")
    private Map<String, String> loadStopBaseNames(Path stopsFile) {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(stopsFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("RS_")) {
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length < 2) {
                    continue;
                }
                String name = cols[1];
                int parenIndex = name.indexOf('(');
                result.put(cols[0], (parenIndex >= 0 ? name.substring(0, parenIndex) : name).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS stops.txt 로드 실패", e);
        }
        return result;
    }

    // stop_times.txt(1800만 행)를 한 줄씩 읽으며 같은 trip_id가 이어지는 동안 모아서 트립 단위로 넘김
    // (GTFS 관례상 같은 trip의 행이 연속해서 나옴을 전제로 함)
    private void streamStopTimesByTrip(Path stopTimesFile, java.util.function.Consumer<List<String[]>> onTrip) {
        String currentTripId = null;
        List<String[]> currentRows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(stopTimesFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 5) {
                    continue;
                }
                if (!cols[0].equals(currentTripId)) {
                    if (!currentRows.isEmpty()) {
                        onTrip.accept(currentRows);
                    }
                    currentTripId = cols[0];
                    currentRows = new ArrayList<>();
                }
                currentRows.add(cols);
            }
            if (!currentRows.isEmpty()) {
                onTrip.accept(currentRows);
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS stop_times.txt 로드 실패", e);
        }
    }

    // "06:19:00" -> 22740(초). 빈 값이면 -1
    private int parseSeconds(String hms) {
        if (hms == null || hms.isBlank()) {
            return -1;
        }
        String[] parts = hms.split(":");
        if (parts.length < 2) {
            return -1;
        }
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return hour * 3600 + minute * 60 + second;
    }
}
