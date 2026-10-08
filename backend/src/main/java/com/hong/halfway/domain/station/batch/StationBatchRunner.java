package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.batch.dto.SeoulStationListResponse;
import com.hong.halfway.domain.station.batch.dto.TagoStationListResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지하철 역 목록 배치 적재. `--spring.profiles.active=local,batch`로 실행할 때만 동작
 * 1~8호선은 서울교통공사(서울 열린데이터광장) 데이터를 기준으로, 나머지 노선은 TAGO로 보충
 * StationTravelTimeBatchRunner가 이 역 목록에 의존하므로 먼저 실행되게 순서 지정
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@Order(1)
public class StationBatchRunner implements CommandLineRunner {

    private static final Set<String> SEOUL_METRO_LINES = Set.of(
            "1호선", "2호선", "3호선", "4호선", "5호선", "6호선", "7호선", "8호선");

    // 수도권이 아닌 노선은 이 앱 대상 밖이라 제외 (대구, 부산 등 지역 노선)
    private static final Set<String> NON_METROPOLITAN_LINES = Set.of(
            "대경선", "동해", "부산김해경전철", "자기부상");

    // TAGO 원본 데이터에 같은 노선이 두 가지 이름으로 중복 등록돼 있어 하나로 합침
    private static final Map<String, String> LINE_ALIAS = Map.ofEntries(
            Map.entry("수인분당선", "수인분당"),
            Map.entry("경의선", "경의중앙"),
            Map.entry("경춘선", "경춘"),
            Map.entry("신분당선", "신분당"),
            Map.entry("공항철도", "공항"),
            Map.entry("인천선", "인천1호선"),
            Map.entry("의정부경전철", "의정부"),
            Map.entry("용인경전철", "에버라인"),
            Map.entry("우이신설경전철", "우이신설"),
            Map.entry("우이신설선", "우이신설"),
            Map.entry("경강선", "경강"),
            Map.entry("김포도시철도", "김포골드라인"));

    // TAGO가 노선에 따라 역명에 "역"을 붙이거나 안 붙이는 게 일관되지 않아서, 다른 노선은 "역" 없이 저장된
    // 같은 환승역이 이름이 안 맞아 환승 엣지(StationGraphBuilder.addTransferEdges)에서 누락되는 걸 방지
    private static final Map<String, String> STATION_NAME_ALIAS = Map.of(
            "김포공항역", "김포공항",
            "부천종합운동장역", "부천종합운동장");

    // 서울 열린데이터광장 역 목록 API가 역명 개정을 반영 안 해서 옛 이름을 그대로 주는 경우
    // (4호선 신길온천 -> 능길, 2021-01-20 개정. 수인분당선과 물리적으로 같은 역이라 이름이 안 맞으면 환승 연결이 끊김)
    private static final Map<String, String> SEOUL_METRO_STATION_NAME_ALIAS = Map.of(
            "신길온천", "능길");

    private final SeoulOpenDataClient seoulOpenDataClient;
    private final TagoClient tagoClient;
    private final StationRepository stationRepository;

    /**
     * 1~8호선(서울교통공사) → 나머지 노선(TAGO) 순서로 역 목록 적재
     */
    @Override
    @Transactional
    public void run(String... args) {
        importSeoulMetroStations();
        importOtherLineStations();
    }

    // 1~8호선 역 적재 (서울 열린데이터광장 기준). FR_CODE는 서울교통공사 운영 노선에만 내려옴
    private void importSeoulMetroStations() {
        int saved = 0;
        int frCodeFilled = 0;
        for (SeoulStationListResponse.Row row : seoulOpenDataClient.fetchStations()) {
            String line = normalizeLine(row.line());
            // 서울 열린데이터광장 역 목록엔 1~8호선 외 노선도 섞여 있어 걸러냄 (나머지는 TAGO+LINE_ALIAS로 통일해서 적재)
            if (!SEOUL_METRO_LINES.contains(line)) {
                continue;
            }
            String name = SEOUL_METRO_STATION_NAME_ALIAS.getOrDefault(row.name(), row.name());
            Station station = stationRepository.findByNameAndLine(name, line).orElse(null);
            if (station == null) {
                station = Station.create(name, line);
                station.updateFrCode(row.frCode());
                stationRepository.save(station);
                saved++;
            } else if (station.getFrCode() == null && row.frCode() != null) {
                station.updateFrCode(row.frCode());
                frCodeFilled++;
            }
        }
        log.info("[완료] 1~8호선 역 적재 - 신규 {}건, 역코드 보강 {}건", saved, frCodeFilled);
    }

    // 1~8호선 외 나머지 노선 역 적재 (TAGO 기준). 지역 노선 제외, 중복 노선명은 LINE_ALIAS로 통일
    // TAGO 역 ID도 같이 저장해서 이 노선들의 막차 시간표(TagoTimetableBatchRunner)를 조회할 수 있게 함
    private void importOtherLineStations() {
        int saved = 0;
        int tagoIdFilled = 0;
        for (TagoStationListResponse.Item item : tagoClient.fetchStations()) {
            String line = LINE_ALIAS.getOrDefault(item.subwayRouteName(), item.subwayRouteName());
            // 1~8호선은 이미 서울교통공사 데이터로 적재했으니 중복 저장 방지
            if (SEOUL_METRO_LINES.contains(line)) {
                continue;
            }
            if (NON_METROPOLITAN_LINES.contains(line)) {
                continue;
            }
            String name = baseName(item.subwayStationName());
            Station station = stationRepository.findByNameAndLine(name, line).orElse(null);
            if (station == null) {
                station = Station.create(name, line);
                station.updateTagoStationId(item.subwayStationId());
                stationRepository.save(station);
                saved++;
            } else if (station.getTagoStationId() == null) {
                station.updateTagoStationId(item.subwayStationId());
                tagoIdFilled++;
            }
        }
        log.info("[완료] 나머지 노선 역 적재 - 신규 {}건, TAGO 역 ID 보강 {}건", saved, tagoIdFilled);
    }

    // 서울 열린데이터광장은 "01호선" 형태라 TAGO의 "1호선" 형태와 맞춰줌
    private String normalizeLine(String line) {
        return line.replaceFirst("^0(\\d)", "$1");
    }

    // TAGO는 "양재(서초구청)"처럼 부기역명을 붙여 같은 역을 중복 제공하는 경우가 있어 제거하고 저장
    // STATION_NAME_ALIAS로 "역" 접미사 불일치도 같이 정리
    private String baseName(String name) {
        int parenIndex = name.indexOf('(');
        String stripped = parenIndex >= 0 ? name.substring(0, parenIndex).trim() : name;
        return STATION_NAME_ALIAS.getOrDefault(stripped, stripped);
    }
}
