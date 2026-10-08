package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.SeoulTimetableResponse;
import com.hong.halfway.domain.lasttrain.repository.TrainTimetableRepository;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 역별 열차 시간표 배치 적재. `--spring.profiles.active=local,batch`로 실행할 때만 동작
 * 서울교통공사 운영 노선(1~9호선)만 대상. 역 x 요일타입(3) x 방향(2) 조합마다 호출해야 해서 호출 수가 많아
 * 조합 하나 끝날 때마다 바로 커밋(JdbcTemplate, 트랜잭션 없음) → 중간에 중단돼도 재실행 시 이미 적재된 조합은 건너뜀
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(3)
public class TrainTimetableBatchRunner implements CommandLineRunner {

    private static final Set<String> TARGET_LINES = Set.of(
            "1호선", "2호선", "3호선", "4호선", "5호선", "6호선", "7호선", "8호선", "9호선");
    private static final int[] WEEK_TAGS = {1, 2, 3};
    private static final int[] INOUT_TAGS = {1, 2};

    private final StationRepository stationRepository;
    private final SeoulTimetableClient seoulTimetableClient;
    private final TrainTimetableRepository trainTimetableRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 1~9호선 역마다 요일타입x방향 조합을 돌며 시간표 적재 (이미 있는 조합은 건너뜀)
     */
    @Override
    public void run(String... args) {
        List<Station> stations = stationRepository.findAllByLineInAndFrCodeIsNotNull(TARGET_LINES);
        int skipped = 0;
        int noData = 0;
        int failed = 0;
        long totalRows = 0;
        long start = System.currentTimeMillis();
        int stationIndex = 0;

        for (Station station : stations) {
            stationIndex++;
            if (stationIndex % 20 == 0) {
                log.info("[진행] 열차 시간표 적재 - {}/{}개 역, 적재행 {}건, 건너뜀 {}건, 데이터없음 {}건, 실패 {}건",
                        stationIndex, stations.size(), totalRows, skipped, noData, failed);
            }
            for (int weekTag : WEEK_TAGS) {
                for (int inoutTag : INOUT_TAGS) {
                    if (trainTimetableRepository.existsByStationIdAndWeekTagAndInoutTag(
                            station.getId(), (short) weekTag, (short) inoutTag)) {
                        skipped++;
                        continue;
                    }

                    List<SeoulTimetableResponse.Row> rows =
                            seoulTimetableClient.fetchTimetable(station.getFrCode(), weekTag, inoutTag);
                    if (rows == null) {
                        failed++;
                        continue;
                    }
                    if (rows.isEmpty()) {
                        noData++;
                        continue;
                    }
                    try {
                        totalRows += insertRows(station, weekTag, inoutTag, rows);
                    } catch (Exception e) {
                        // 한 조합의 적재 실패가 전체 배치를 중단시키지 않도록 격리
                        log.error("[에러] 시간표 저장 실패 - station: {}({}), weekTag: {}, inoutTag: {}",
                                station.getName(), station.getLine(), weekTag, inoutTag, e);
                        failed++;
                    }
                }
            }
        }

        log.info("[완료] 열차 시간표 적재 - 대상역 {}개, 적재행 {}건, 건너뜀(기적재) {}건, 데이터없음 {}건, 실패 {}건, {}ms",
                stations.size(), totalRows, skipped, noData, failed, System.currentTimeMillis() - start);
    }

    // 한 (역, 요일타입, 방향) 조합의 응답을 train_timetable에 insert
    private int insertRows(Station station, int weekTag, int inoutTag, List<SeoulTimetableResponse.Row> rows) {
        List<Object[]> batch = new ArrayList<>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (SeoulTimetableResponse.Row row : rows) {
            Short arriveMinute = parseMinutes(row.arriveTime());
            Short leaveMinute = parseMinutes(row.leftTime());
            batch.add(new Object[]{
                    station.getId(), row.trainNo(), (short) weekTag, (short) inoutTag,
                    arriveMinute, leaveMinute, row.originStationName(), row.destStationName(), row.expressYn(),
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

    // "24:46:00" -> 1486 (분). "00:00:00"은 시발역 도착기록·종착역 출발기록이 없다는 placeholder라 null
    private Short parseMinutes(String hms) {
        if ("00:00:00".equals(hms)) {
            return null;
        }
        String[] parts = hms.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return (short) (hour * 60 + minute);
    }
}
