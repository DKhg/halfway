package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.station.batch.GtfsDirectoryLocator;
import com.hong.halfway.domain.station.batch.GtfsLineResolver;
import com.hong.halfway.domain.station.batch.GtfsLineResolver.RouteInfo;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TAGO가 담당하던 노선의 평일 시간표를 국가교통DB(KTDB) 표준 GTFS로 교체 적재.
 * `--spring.profiles.active=local,batch`로 실행할 때만 동작.
 * GTFS 폴더 경로는 {@link GtfsDirectoryLocator}가 읽는 로컬 포인터 파일에 한 줄로 적어둠
 * GTFS는 trip_id가 실제 물리적 열차를 가리켜서 TAGO처럼 순위를 추정할 필요가 없음
 * 다만 이 GTFS는 2025년 3월 평일 1일 스냅샷이라 토요일·휴일 시간표는 없음(TagoTimetableBatchRunner가 계속 담당)
 * 1~8호선은 서울교통공사 데이터로 시간표가 이미 충분해서 대상에서 제외. 9호선은 서울교통공사가 아니라
 * 민간 운영사(1단계) 포함이라 station.fr_code가 없어서(StationBatchRunner 참고) 평일 시간표를 여기서 담당 —
 * 역간 소요시간 그래프 쪽 GTFS 보강은 StationGraphBuilder·GtfsAdjacentTimeLoader가 따로 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(5)
public class GtfsWeekdayTimetableBatchRunner implements CommandLineRunner {

    private static final short WEEKDAY_TAG = 1;

    private final StationRepository stationRepository;
    private final GtfsDirectoryLocator gtfsDirectoryLocator;
    private final JdbcTemplate jdbcTemplate;

    /**
     * GTFS 폴더에서 대상 노선의 trip_id별 역-시각 시퀀스를 읽어 평일 train_timetable 재적재
     */
    @Override
    public void run(String... args) {
        Path dir = gtfsDirectoryLocator.locate().orElse(null);
        if (dir == null) {
            return;
        }

        Map<String, RouteInfo> routeInfoByRouteId = loadTargetRoutes(dir.resolve("routes.txt"));
        if (routeInfoByRouteId.isEmpty()) {
            log.warn("[건너뜀] GTFS routes.txt에서 대상 노선을 찾지 못함");
            return;
        }
        Map<String, RouteInfo> routeInfoByTripId = loadTripRoutes(dir.resolve("trips.txt"), routeInfoByRouteId);
        Map<String, String> baseNameByStopId = loadStopBaseNames(dir.resolve("stops.txt"));

        Map<String, Station> stationByNameAndLine = new HashMap<>();
        for (Station station : stationRepository.findAll()) {
            stationByNameAndLine.put(station.getName() + "|" + station.getLine(), station);
        }
        Set<String> targetLines = new HashSet<>(routeInfoByRouteId.values().stream().map(RouteInfo::line).toList());

        deleteExistingWeekdayRows(targetLines);

        long inserted = streamStopTimesAndInsert(dir.resolve("stop_times.txt"), routeInfoByTripId, baseNameByStopId, stationByNameAndLine);
        log.info("[완료] GTFS 평일 시간표 적재 - {}개 노선, {}행", targetLines.size(), inserted);
    }

    // routes.txt에서 대상 노선만 추출.
    // 9호선은 서울교통공사가 아니라 민간 운영사(1단계) 포함이라 station.fr_code가 전혀 채워지지 않음
    // (StationBatchRunner.SEOUL_METRO_LINES가 1~8호선까지만 포함) - 평일 시간표는 GTFS가 전담
    private Map<String, RouteInfo> loadTargetRoutes(Path routesFile) {
        Map<String, RouteInfo> result = new HashMap<>();
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
        log.info("[진행] GTFS 대상 노선 {}개 추출", result.size());
        return result;
    }

    // trips.txt에서 대상 route_id에 속하는 trip_id만 추려 RouteInfo와 연결
    private Map<String, RouteInfo> loadTripRoutes(Path tripsFile, Map<String, RouteInfo> routeInfoByRouteId) {
        Map<String, RouteInfo> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(tripsFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 3) {
                    continue;
                }
                RouteInfo info = routeInfoByRouteId.get(cols[0]);
                if (info != null) {
                    result.put(cols[2], info);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS trips.txt 로드 실패", e);
        }
        log.info("[진행] GTFS 대상 trip {}개 추출", result.size());
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
                String baseName = (parenIndex >= 0 ? name.substring(0, parenIndex) : name).trim();
                result.put(cols[0], baseName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("GTFS stops.txt 로드 실패", e);
        }
        return result;
    }

    // 대상 노선 역의 기존 평일(week_tag=1) 행 삭제 후 GTFS로 재적재
    private void deleteExistingWeekdayRows(Set<String> targetLines) {
        if (targetLines.isEmpty()) {
            return;
        }
        String placeholders = targetLines.stream().map(l -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(WEEKDAY_TAG);
        params.addAll(targetLines);
        int deleted = jdbcTemplate.update(
                "DELETE FROM train_timetable WHERE week_tag = ? AND station_id IN "
                        + "(SELECT id FROM station WHERE line IN (" + placeholders + "))",
                params.toArray());
        log.info("[진행] 기존 GTFS 대상 노선 평일 행 삭제 - {}건", deleted);
    }

    // stop_times.txt(1800만 행)를 한 줄씩 읽으며 대상 trip만 필터링해서 역별로 insert
    private long streamStopTimesAndInsert(Path stopTimesFile, Map<String, RouteInfo> routeInfoByTripId,
            Map<String, String> baseNameByStopId, Map<String, Station> stationByNameAndLine) {
        List<Object[]> batch = new ArrayList<>();
        long inserted = 0;
        long scanned = 0;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        try (BufferedReader reader = Files.newBufferedReader(stopTimesFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                scanned++;
                String[] cols = line.split(",");
                if (cols.length < 4) {
                    continue;
                }
                RouteInfo info = routeInfoByTripId.get(cols[0]);
                if (info == null) {
                    continue;
                }
                String baseName = baseNameByStopId.get(cols[3]);
                if (baseName == null) {
                    continue;
                }
                Station station = stationByNameAndLine.get(baseName + "|" + info.line());
                if (station == null) {
                    continue;
                }

                Short arriveMinute = parseMinutes(cols[1]);
                Short leaveMinute = parseMinutes(cols[2]);
                batch.add(new Object[]{
                        station.getId(), cols[0], WEEKDAY_TAG, info.inoutTag(),
                        arriveMinute, leaveMinute, "GTFS", "GTFS", null,
                        now, now
                });
                inserted++;
                if (batch.size() >= 1000) {
                    flush(batch);
                }
                if (scanned % 2_000_000 == 0) {
                    log.info("[진행] GTFS stop_times {}행 스캔, {}행 적재", scanned, inserted);
                }
            }
            flush(batch);
        } catch (IOException e) {
            throw new IllegalStateException("GTFS stop_times.txt 로드 실패", e);
        }
        return inserted;
    }

    private void flush(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO train_timetable "
                        + "(station_id, train_no, week_tag, inout_tag, arrive_minute, leave_minute, "
                        + "origin_station_name, dest_station_name, express_yn, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batch);
        batch.clear();
    }

    // "06:19:00" -> 379(분). 자정 넘는 시각은 "25:10:00"처럼 24시를 넘겨 그대로 옴 (기존 분단위 표현과 호환)
    private Short parseMinutes(String hms) {
        if (hms == null || hms.isBlank()) {
            return null;
        }
        String[] parts = hms.split(":");
        if (parts.length < 2) {
            return null;
        }
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return (short) (hour * 60 + minute);
    }
}
