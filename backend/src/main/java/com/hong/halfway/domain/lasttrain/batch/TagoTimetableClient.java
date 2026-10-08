package com.hong.halfway.domain.lasttrain.batch;

import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * TAGO 역 ID로 열차 시간표 조회 (서울교통공사 외 노선). 역 하나당 요일타입×방향 조합으로 여러 번 호출해야 해서
 * 호출 간 지연(rate limit)과 실패 시 재시도(backoff)를 포함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagoTimetableClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/SubwayInfo";
    private static final int PAGE_SIZE = 500;

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500;
    // 호출 사이 기본 지연 (공공 API 과호출 방지)
    private static final long THROTTLE_MS = 150;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    /**
     * 역 ID+요일타입+방향 조합으로 전체 열차 시간표 조회. 실패 시 지수 백오프로 재시도
     */
    public List<TagoTimetableResponse.Item> fetchTimetable(String tagoStationId, String dailyTypeCode,
            String upDownTypeCode) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                List<TagoTimetableResponse.Item> items = fetchAllPages(tagoStationId, dailyTypeCode, upDownTypeCode);
                sleep(THROTTLE_MS);
                return items;
            } catch (Exception e) {
                log.error("[에러] TAGO 시간표 조회 실패 (시도 {}/{}) - stationId: {}, dailyTypeCode: {}, upDownTypeCode: {}",
                        attempt, MAX_ATTEMPTS, tagoStationId, dailyTypeCode, upDownTypeCode, e);
                if (attempt == MAX_ATTEMPTS) {
                    return null;
                }
                sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
            }
        }
        return null;
    }

    // 전체 페이지를 끝까지 조회해서 합침
    private List<TagoTimetableResponse.Item> fetchAllPages(String tagoStationId, String dailyTypeCode,
            String upDownTypeCode) {
        List<TagoTimetableResponse.Item> allItems = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            TagoTimetableResponse.Body body = fetchPage(tagoStationId, dailyTypeCode, upDownTypeCode, pageNo);
            if (body == null || body.items() == null || body.items().item() == null || body.items().item().isEmpty()) {
                break;
            }
            allItems.addAll(body.items().item());
            if (allItems.size() >= body.totalCount()) {
                break;
            }
            pageNo++;
        }
        return allItems;
    }

    private TagoTimetableResponse.Body fetchPage(String tagoStationId, String dailyTypeCode, String upDownTypeCode,
            int pageNo) {
        String key = properties.tago().key();
        TagoTimetableResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/GetSubwaySttnAcctoSchdulList")
                        .queryParam("serviceKey", key)
                        .queryParam("_type", "json")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", PAGE_SIZE)
                        .queryParam("subwayStationId", tagoStationId)
                        .queryParam("dailyTypeCode", dailyTypeCode)
                        .queryParam("upDownTypeCode", upDownTypeCode)
                        .build())
                .retrieve()
                .body(TagoTimetableResponse.class);

        if (response == null || response.response() == null
                || !"00".equals(response.response().header().resultCode())) {
            return null;
        }
        return response.response().body();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
