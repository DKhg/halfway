package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.batch.dto.ManualLineOrder;
import com.hong.halfway.domain.station.batch.dto.SeoulStationTimeResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역 그래프를 만들어 모든 역 쌍의 최단 이동시간·경유역(predecessor)을 계산, station_travel_time을 통째로 재생성
 * predecessor는 막차 조회 시점에 경로를 복원해 환승역 후보를 검증하는 데 쓰임 (LastTrainService)
 * StationBatchRunner(역 목록 적재) 다음에 실행돼야 해서 순서를 뒤로 둠
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(2)
public class StationTravelTimeBatchRunner implements CommandLineRunner {

    private static final int JDBC_BATCH_SIZE = 1000;

    private final StationRepository stationRepository;
    private final SeoulOpenDataClient seoulOpenDataClient;
    private final StationGraphBuilder graphBuilder;
    private final ShortestPathCalculator shortestPathCalculator;
    private final ManualLineOrderLoader manualLineOrderLoader;
    private final GtfsDirectoryLocator gtfsDirectoryLocator;
    private final GtfsAdjacentTimeLoader gtfsAdjacentTimeLoader;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 그래프 구성 → 역마다 다익스트라 실행 → station_travel_time 전체 재생성
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        List<Station> stations = stationRepository.findAll();
        List<SeoulStationTimeResponse.Row> seoulRows = seoulOpenDataClient.fetchTravelTimes();
        List<ManualLineOrder> manualLines = manualLineOrderLoader.load();
        GtfsAdjacentTimeLoader.Result gtfsResult = gtfsDirectoryLocator.locate()
                .map(dir -> gtfsAdjacentTimeLoader.load(dir, stations))
                .orElse(new GtfsAdjacentTimeLoader.Result(List.of(), Set.of()));

        Map<Long, List<Edge>> graph = graphBuilder.build(stations, seoulRows, manualLines, gtfsResult);

        jdbcTemplate.update("DELETE FROM station_travel_time");

        List<Object[]> batch = new ArrayList<>();
        long now = System.currentTimeMillis();
        int totalPairs = 0;
        for (Station source : stations) {
            Map<Long, ShortestPathCalculator.Result> results = shortestPathCalculator.calculate(source.getId(), graph);
            for (Map.Entry<Long, ShortestPathCalculator.Result> entry : results.entrySet()) {
                short minutes = (short) Math.round(entry.getValue().totalSeconds() / 60.0);
                batch.add(new Object[]{
                        source.getId(), entry.getKey(),
                        minutes, (short) entry.getValue().transfers(), entry.getValue().predecessorStationId(),
                        Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
                });
                totalPairs++;
                if (batch.size() >= JDBC_BATCH_SIZE) {
                    flush(batch);
                }
            }
        }
        flush(batch);

        log.info("[완료] 역간 최단시간 계산 - {}개 역, {}쌍, {}ms",
                stations.size(), totalPairs, System.currentTimeMillis() - now);
    }

    // 모아둔 배치를 JDBC 배치 insert로 한 번에 반영 (JPA로 건건이 저장하면 너무 느림)
    private void flush(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO station_travel_time (from_station_id, to_station_id, minutes, transfers, predecessor_station_id, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                batch);
        batch.clear();
    }
}
