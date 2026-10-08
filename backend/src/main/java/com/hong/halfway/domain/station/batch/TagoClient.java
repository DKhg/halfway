package com.hong.halfway.domain.station.batch;

import com.hong.halfway.domain.station.batch.dto.TagoStationListResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * TAGO(국토교통부) 지하철 역 정보 조회 (전국 노선)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagoClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/SubwayInfo";
    private static final int PAGE_SIZE = 1000;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    // 전체 역 목록을 페이지 단위로 끝까지 조회해서 합침
    public List<TagoStationListResponse.Item> fetchStations() {
        List<TagoStationListResponse.Item> allItems = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            TagoStationListResponse.Body body = fetchPage(pageNo);
            if (body == null || body.items() == null || body.items().item() == null || body.items().item().isEmpty()) {
                break;
            }
            allItems.addAll(body.items().item());
            if (allItems.size() >= body.totalCount()) {
                break;
            }
            pageNo++;
        }

        log.info("[완료] TAGO 역 목록 조회 - 수신: {}", allItems.size());
        return allItems;
    }

    // 한 페이지 조회, 실패하면 null
    private TagoStationListResponse.Body fetchPage(int pageNo) {
        String key = properties.tago().key();
        TagoStationListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/GetKwrdFndSubwaySttnList")
                        .queryParam("serviceKey", key)
                        .queryParam("_type", "json")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", PAGE_SIZE)
                        .build())
                .retrieve()
                .body(TagoStationListResponse.class);

        if (response == null || response.response() == null
                || !"00".equals(response.response().header().resultCode())) {
            log.error("[에러] TAGO 역 목록 조회 실패 - page: {}", pageNo);
            return null;
        }
        return response.response().body();
    }
}
