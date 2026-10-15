package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.SeoulTimetableResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.ArrayList;
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
    // 한 페이지당 요청 건수. 역당 최대 열차 수(실측 최대 246건)보다 넉넉하다고 가정했었는데,
    // 2호선 본선+성수지선이 겹치는 성수역처럼 500건을 넘는 역이 있어서 list_total_count를 보고 다음 페이지까지 이어받음
    private static final int PAGE_ROWS = 500;

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500;
    // 호출 사이 기본 지연 (공공 API 과호출 방지)
    private static final long THROTTLE_MS = 150;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.create();

    /**
     * 역코드+요일타입+방향 조합으로 전체 열차 시간표 조회. list_total_count가 한 페이지(PAGE_ROWS)를 넘으면
     * 다음 페이지까지 이어서 조회. 실패 시 지수 백오프로 재시도, 이미 받아둔 페이지가 있으면 그것까지는 반환
     */
    public List<SeoulTimetableResponse.Row> fetchTimetable(String frCode, int weekTag, int inoutTag) {
        List<SeoulTimetableResponse.Row> allRows = new ArrayList<>();
        int start = 1;
        while (true) {
            int end = start + PAGE_ROWS - 1;
            SeoulTimetableResponse.Body page = fetchPage(frCode, weekTag, inoutTag, start, end);
            if (page == null) {
                return allRows.isEmpty() ? null : allRows;
            }
            List<SeoulTimetableResponse.Row> rows = page.row();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            allRows.addAll(rows);
            if (allRows.size() >= page.totalCount() || rows.size() < PAGE_ROWS) {
                break;
            }
            log.info("[진행] 시간표 페이지 추가 조회 - frCode: {}, weekTag: {}, inoutTag: {}, 누적 {}/{}건",
                    frCode, weekTag, inoutTag, allRows.size(), page.totalCount());
            start += PAGE_ROWS;
        }
        return allRows;
    }

    // 한 페이지 조회, 실패 시 지수 백오프로 재시도. 해당 역코드에 시간표가 없는 경우(INFO-200)도 정상 응답이라 빈 목록으로 반환
    private SeoulTimetableResponse.Body fetchPage(String frCode, int weekTag, int inoutTag, int start, int end) {
        String key = properties.seoulOpenData().subwayKey();
        String uri = BASE_URL + "/" + key + "/json/" + SERVICE + "/" + start + "/" + end + "/"
                + frCode + "/" + weekTag + "/" + inoutTag + "/";

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SeoulTimetableResponse response = restClient.get().uri(uri).retrieve().body(SeoulTimetableResponse.class);
                sleep(THROTTLE_MS);
                if (response == null || response.body() == null) {
                    return new SeoulTimetableResponse.Body(0, List.of());
                }
                return response.body();
            } catch (Exception e) {
                log.error("[에러] 시간표 조회 실패 (시도 {}/{}) - frCode: {}, weekTag: {}, inoutTag: {}, start: {}",
                        attempt, MAX_ATTEMPTS, frCode, weekTag, inoutTag, start, e);
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
