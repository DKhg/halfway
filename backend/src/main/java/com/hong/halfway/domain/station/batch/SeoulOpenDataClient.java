package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.batch.dto.SeoulStationListResponse;
import com.hong.halfway.domain.station.batch.dto.SeoulStationTimeResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 서울 열린데이터광장 지하철 역 정보 조회 (1~8호선)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulOpenDataClient {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";
    // 1~8호선 전체 역이 1000개를 안 넘어서 한 번에 조회
    private static final int MAX_ROWS = 1000;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.create();

    // 1~8호선 역 목록 (좌표는 미포함)
    public List<SeoulStationListResponse.Row> fetchStations() {
        String key = properties.seoulOpenData().generalKey();
        SeoulStationListResponse response = restClient.get()
                .uri(BASE_URL + "/{key}/json/SearchSTNBySubwayLineInfo/1/{maxRows}/", key, MAX_ROWS)
                .retrieve()
                .body(SeoulStationListResponse.class);

        if (response == null || response.body() == null) {
            log.error("[에러] 서울 열린데이터광장 역 목록 응답 형식이 올바르지 않음");
            return List.of();
        }

        log.info("[완료] 서울 열린데이터광장 역 목록 조회 - 전체: {}, 수신: {}",
                response.body().totalCount(), response.body().row().size());
        return response.body().row();
    }

    // 1~8호선 인접역 운행시간 (일부 구간만 제공, 나머지는 수동 데이터로 보충)
    public List<SeoulStationTimeResponse.Row> fetchTravelTimes() {
        String key = properties.seoulOpenData().generalKey();
        SeoulStationTimeResponse response = restClient.get()
                .uri(BASE_URL + "/{key}/json/StationDstncReqreTimeHm/1/{maxRows}/", key, MAX_ROWS)
                .retrieve()
                .body(SeoulStationTimeResponse.class);

        if (response == null || response.body() == null) {
            log.error("[에러] 서울 열린데이터광장 역간 소요시간 응답 형식이 올바르지 않음");
            return List.of();
        }

        log.info("[완료] 서울 열린데이터광장 역간 소요시간 조회 - 전체: {}, 수신: {}",
                response.body().totalCount(), response.body().row().size());
        return response.body().row();
    }
}
