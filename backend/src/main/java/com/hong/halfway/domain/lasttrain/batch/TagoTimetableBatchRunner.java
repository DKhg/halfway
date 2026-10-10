package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.AssignedTrip;
import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.StationItems;
import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse;
import com.hong.halfway.domain.lasttrain.repository.TrainTimetableRepository;
import com.hong.halfway.domain.station.batch.ManualLineOrderLoader;
import com.hong.halfway.domain.station.batch.dto.ManualLineOrder;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 역별 열차 시간표 배치 적재 (서울교통공사 외 노선, TAGO 역 ID 있는 역 대상). 토요일·휴일만 담당
 * (평일은 {@link GtfsWeekdayTimetableBatchRunner}가 실제 trip_id가 있는 GTFS로 담당).
 * `--spring.profiles.active=local,batch`로 실행할 때만 동작.
 * TAGO 응답엔 열차번호가 없어서, 노선 전체를 물리적 순서(manual-lines.json)대로 모아 {@link TagoTripAssigner}로
 * 체이닝 방식 합성 식별자를 만듦 — 역마다 독립적으로 순위를 매기면 중간역 시종착 열차 때문에
 * 순위가 어긋나는 문제가 있어서, 노선 단위로 처리하도록 역 하나씩 처리하던 이전 방식에서 바꿈
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(4)
public class TagoTimetableBatchRunner implements CommandLineRunner {

    // 평일(01)은 GtfsWeekdayTimetableBatchRunner가 담당(실제 trip_id가 있어 더 정확함). 여긴 토요일·휴일만
    private static final String[] DAILY_TYPE_CODES = {"02", "03"};
    private static final String[] UP_DOWN_TYPE_CODES = {"U", "D"};
    private static final Map<String, Short> WEEK_TAG_BY_DAILY_TYPE_CODE = Map.of("02", (short) 2, "03", (short) 3);
    private static final Map<String, Short> INOUT_TAG_BY_UP_DOWN_TYPE_CODE = Map.of("U", (short) 1, "D", (short) 2);

    private final StationRepository stationRepository;
    private final ManualLineOrderLoader manualLineOrderLoader;
    private final TagoTimetableClient tagoTimetableClient;
    private final TrainTimetableRepository trainTimetableRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * manual-lines.json의 노선 구간마다, TAGO 역 ID가 있는 역들을 물리적 순서로 묶어 요일타입×방향별로 적재
     */
    @Override
    public void run(String... args) {
        List<Station> tagoStations = stationRepository.findAllByTagoStationIdIsNotNull();
        List<ManualLineOrder> manualLines = manualLineOrderLoader.load();

        long totalRows = 0;
        int segmentCount = 0;
        long start = System.currentTimeMillis();

        for (ManualLineOrder lineOrder : manualLines) {
            List<Station> ordered = resolveOrderedStations(lineOrder, tagoStations);
            if (ordered.size() < 2) {
                continue;
            }
            segmentCount++;

            for (String dailyTypeCode : DAILY_TYPE_CODES) {
                short weekTag = WEEK_TAG_BY_DAILY_TYPE_CODE.get(dailyTypeCode);
                for (String upDownTypeCode : UP_DOWN_TYPE_CODES) {
                    short inoutTag = INOUT_TAG_BY_UP_DOWN_TYPE_CODE.get(upDownTypeCode);
                    if (isFullyLoaded(ordered, weekTag, inoutTag)) {
                        continue;
                    }
                    totalRows += loadSegmentCombo(ordered, dailyTypeCode, upDownTypeCode, weekTag, inoutTag);
                }
            }
            log.info("[진행] TAGO 열차 시간표 적재 - {}개 노선 구간 처리, 누적 적재행 {}건", segmentCount, totalRows);
        }

        log.info("[완료] TAGO 열차 시간표 적재 - {}개 노선 구간, 적재행 {}건, {}ms",
                segmentCount, totalRows, System.currentTimeMillis() - start);
    }

    // manual-lines.json 구간의 역 이름 순서를, 실제 TAGO 역 ID가 있는 Station 엔티티로 치환 (못 찾으면 그 역만 건너뜀)
    private List<Station> resolveOrderedStations(ManualLineOrder lineOrder, List<Station> tagoStations) {
        List<Station> result = new ArrayList<>();
        for (ManualLineOrder.StationEntry entry : lineOrder.stations()) {
            tagoStations.stream()
                    .filter(s -> s.getName().equals(entry.name()) && s.getLine().equals(lineOrder.line()))
                    .findFirst()
                    .ifPresent(result::add);
        }
        return result;
    }

    // 구간의 역 전부가 이미 이 요일타입×방향으로 적재돼 있으면 재호출을 건너뜀 (배치 재시작 시 중복 호출 방지)
    private boolean isFullyLoaded(List<Station> ordered, short weekTag, short inoutTag) {
        return ordered.stream()
                .allMatch(s -> trainTimetableRepository.existsByStationIdAndWeekTagAndInoutTag(s.getId(), weekTag, inoutTag));
    }

    // 구간 전체의 원본 시간표를 역마다 조회하고, 실제 운행 방향에 맞게 정렬해서 체이닝 적재
    private int loadSegmentCombo(List<Station> ordered, String dailyTypeCode, String upDownTypeCode,
            short weekTag, short inoutTag) {
        Map<Station, List<TagoTimetableResponse.Item>> itemsByStation = new LinkedHashMap<>();
        for (Station station : ordered) {
            List<TagoTimetableResponse.Item> items =
                    tagoTimetableClient.fetchTimetable(station.getTagoStationId(), dailyTypeCode, upDownTypeCode);
            if (items != null && !items.isEmpty()) {
                itemsByStation.put(station, items);
            }
        }
        if (itemsByStation.isEmpty()) {
            return 0;
        }

        List<Station> effectiveOrder = new ArrayList<>(itemsByStation.keySet());
        if (!isForwardOrder(ordered, itemsByStation)) {
            Collections.reverse(effectiveOrder);
        }

        List<StationItems> stationItems = effectiveOrder.stream()
                .map(s -> new StationItems(s, itemsByStation.get(s)))
                .toList();
        Map<Station, List<AssignedTrip>> assignedByStation = TagoTripAssigner.assignAlongLine(stationItems);

        int inserted = 0;
        for (Map.Entry<Station, List<AssignedTrip>> entry : assignedByStation.entrySet()) {
            try {
                inserted += insertRows(entry.getKey(), weekTag, inoutTag, entry.getValue());
            } catch (Exception e) {
                log.error("[에러] TAGO 시간표 저장 실패 - station: {}({})", entry.getKey().getName(), entry.getKey().getLine(), e);
            }
        }
        return inserted;
    }

    // manual-lines.json에 적힌 역 나열 순서가 실제 운행 방향과 같은 방향인지 판별.
    // 데이터가 있는 첫 역과 마지막 역의 가장 빠른 출발시각을 비교해서, 나열 순서대로 시각이 늘어나면 정방향으로 봄
    private boolean isForwardOrder(List<Station> fullOrder, Map<Station, List<TagoTimetableResponse.Item>> itemsByStation) {
        Station first = fullOrder.stream().filter(itemsByStation::containsKey).findFirst().orElse(null);
        Station last = null;
        for (int i = fullOrder.size() - 1; i >= 0; i--) {
            if (itemsByStation.containsKey(fullOrder.get(i))) {
                last = fullOrder.get(i);
                break;
            }
        }
        if (first == null || last == null || first.equals(last)) {
            return true;
        }
        int firstEarliest = earliestDepMinutes(itemsByStation.get(first));
        int lastEarliest = earliestDepMinutes(itemsByStation.get(last));
        return firstEarliest <= lastEarliest;
    }

    private int earliestDepMinutes(List<TagoTimetableResponse.Item> items) {
        return items.stream().map(this::representativeTime).filter(this::isValidTime)
                .min(Comparator.naturalOrder())
                .map(this::parseMinutesUnsigned).orElse(0);
    }

    // depTime이 없거나 "0"(이 방향 종착역이라 출발기록이 없다는 placeholder)이면 arrTime을 대신 씀
    private String representativeTime(TagoTimetableResponse.Item item) {
        return isValidTime(item.depTime()) ? item.depTime() : item.arrTime();
    }

    private boolean isValidTime(String hhmmss) {
        return hhmmss != null && hhmmss.length() >= 4 && !"0".equals(hhmmss);
    }

    private int parseMinutesUnsigned(String hhmmss) {
        int hour = Integer.parseInt(hhmmss.substring(0, 2));
        int minute = Integer.parseInt(hhmmss.substring(2, 4));
        return hour * 60 + minute;
    }

    // 한 역의 합성 trip을 train_timetable에 insert
    private int insertRows(Station station, short weekTag, short inoutTag, List<AssignedTrip> trips) {
        List<Object[]> batch = new ArrayList<>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (AssignedTrip trip : trips) {
            Short arriveMinute = parseMinutes(trip.arrTime());
            Short leaveMinute = parseMinutes(trip.depTime());
            batch.add(new Object[]{
                    station.getId(), trip.trainNo(), weekTag, inoutTag,
                    // TAGO는 시발역(출발지) 정보를 안 줘서 origin_station_name은 의미 있는 값이 없음(NOT NULL이라 placeholder)
                    arriveMinute, leaveMinute, "TAGO", trip.destStationName(), null,
                    now, now
            });
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO train_timetable "
                        + "(station_id, train_no, week_tag, inout_tag, arrive_minute, leave_minute, "
                        + "origin_station_name, dest_station_name, express_yn, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batch);
        return batch.size();
    }

    // "233900" -> 1439(분). "0"은 해당 방향에 시발역이라 도착기록이 없다는 placeholder라 null
    private Short parseMinutes(String hhmmss) {
        if (hhmmss == null || hhmmss.isBlank() || "0".equals(hhmmss)) {
            return null;
        }
        int hour = Integer.parseInt(hhmmss.substring(0, 2));
        int minute = Integer.parseInt(hhmmss.substring(2, 4));
        return (short) (hour * 60 + minute);
    }
}
