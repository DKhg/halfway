package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.AssignedTrip;
import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse;
import com.hong.halfway.domain.lasttrain.repository.TrainTimetableRepository;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * 역별 열차 시간표 배치 적재 (서울교통공사 외 노선, TAGO 역 ID 있는 역 대상).
 * `--spring.profiles.active=local,batch`로 실행할 때만 동작.
 * TAGO 응답엔 열차번호가 없어서 {@link TagoTripAssigner}로 만든 합성 식별자를 train_no에 그대로 저장함 —
 * 그러면 LastTrainService·TrainTimetableRepository는 서울교통공사 데이터와 구분 없이 동일하게 조회 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(4)
public class TagoTimetableBatchRunner implements CommandLineRunner {

    private static final String[] DAILY_TYPE_CODES = {"01", "02", "03"};
    private static final String[] UP_DOWN_TYPE_CODES = {"U", "D"};
    private static final Map<String, Short> WEEK_TAG_BY_DAILY_TYPE_CODE = Map.of("01", (short) 1, "02", (short) 2, "03", (short) 3);
    private static final Map<String, Short> INOUT_TAG_BY_UP_DOWN_TYPE_CODE = Map.of("U", (short) 1, "D", (short) 2);

    private final StationRepository stationRepository;
    private final TagoTimetableClient tagoTimetableClient;
    private final TrainTimetableRepository trainTimetableRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * TAGO 역 ID가 있는 역마다 요일타입×방향 조합을 돌며 시간표 적재 (이미 있는 조합은 건너뜀)
     */
    @Override
    public void run(String... args) {
        List<Station> stations = stationRepository.findAllByTagoStationIdIsNotNull();
        int skipped = 0;
        int noData = 0;
        int failed = 0;
        long totalRows = 0;
        long start = System.currentTimeMillis();
        int stationIndex = 0;

        for (Station station : stations) {
            stationIndex++;
            if (stationIndex % 20 == 0) {
                log.info("[진행] TAGO 열차 시간표 적재 - {}/{}개 역, 적재행 {}건, 건너뜀 {}건, 데이터없음 {}건, 실패 {}건",
                        stationIndex, stations.size(), totalRows, skipped, noData, failed);
            }
            for (String dailyTypeCode : DAILY_TYPE_CODES) {
                short weekTag = WEEK_TAG_BY_DAILY_TYPE_CODE.get(dailyTypeCode);
                for (String upDownTypeCode : UP_DOWN_TYPE_CODES) {
                    short inoutTag = INOUT_TAG_BY_UP_DOWN_TYPE_CODE.get(upDownTypeCode);
                    if (trainTimetableRepository.existsByStationIdAndWeekTagAndInoutTag(station.getId(), weekTag, inoutTag)) {
                        skipped++;
                        continue;
                    }

                    List<TagoTimetableResponse.Item> items =
                            tagoTimetableClient.fetchTimetable(station.getTagoStationId(), dailyTypeCode, upDownTypeCode);
                    if (items == null) {
                        failed++;
                        continue;
                    }
                    if (items.isEmpty()) {
                        noData++;
                        continue;
                    }
                    try {
                        totalRows += insertRows(station, weekTag, inoutTag, TagoTripAssigner.assign(items));
                    } catch (Exception e) {
                        // 한 조합의 적재 실패가 전체 배치를 중단시키지 않도록 격리
                        log.error("[에러] TAGO 시간표 저장 실패 - station: {}({}), dailyTypeCode: {}, upDownTypeCode: {}",
                                station.getName(), station.getLine(), dailyTypeCode, upDownTypeCode, e);
                        failed++;
                    }
                }
            }
        }

        log.info("[완료] TAGO 열차 시간표 적재 - 대상역 {}개, 적재행 {}건, 건너뜀(기적재) {}건, 데이터없음 {}건, 실패 {}건, {}ms",
                stations.size(), totalRows, skipped, noData, failed, System.currentTimeMillis() - start);
    }

    // 한 (역, 요일타입, 방향) 조합의 합성 trip을 train_timetable에 insert
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
