package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.SeoulTimetableResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 서울교통공사 역코드로 열차 시간표 조회. 역 하나당 요일타입×방향 조합으로 여러 번 호출해야 해서
 * 호출 간 지연(rate limit)과 실패 시 재시도(backoff)를 포함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulTimetableClient {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";
    private static final String SERVICE = "SearchSTNTimeTableByFRCodeService";
    // 역당 최대 열차 수(실측 최대 246건)보다 넉넉하게
    private static final int MAX_ROWS = 500;

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500;
    // 호출 사이 기본 지연 (공공 API 과호출 방지)
    private static final long THROTTLE_MS = 150;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.create();

    /**
     * 역코드+요일타입+방향 조합으로 전체 열차 시간표 조회. 실패 시 지수 백오프로 재시도
     */
    public List<SeoulTimetableResponse.Row> fetchTimetable(String frCode, int weekTag, int inoutTag) {
        String key = properties.seoulOpenData().subwayKey();
        String uri = BASE_URL + "/" + key + "/json/" + SERVICE + "/1/" + MAX_ROWS + "/"
                + frCode + "/" + weekTag + "/" + inoutTag + "/";

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SeoulTimetableResponse response = restClient.get().uri(uri).retrieve().body(SeoulTimetableResponse.class);
                sleep(THROTTLE_MS);
                if (response == null || response.body() == null || response.body().row() == null) {
                    // 해당 역코드에 시간표가 없는 경우(INFO-200)도 정상 응답이라 빈 목록 반환
                    return List.of();
                }
                return response.body().row();
            } catch (Exception e) {
                log.error("[에러] 시간표 조회 실패 (시도 {}/{}) - frCode: {}, weekTag: {}, inoutTag: {}",
                        attempt, MAX_ATTEMPTS, frCode, weekTag, inoutTag, e);
                if (attempt == MAX_ATTEMPTS) {
                    return null;
                }
                sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
            }
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
