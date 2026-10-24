package com.hong.halfway.domain.station.client;

import com.hong.halfway.domain.station.client.dto.KakaoPlaceSearchResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * 카카오 로컬 API로 지하철역 좌표·주변 가게를 조회. 역이 실제로 확정되거나 가게 목록이 필요할 때만 호출됨(전체 배치 아님)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private static final String BASE_URL = "https://dapi.kakao.com";

    // 카카오 로컬 API 카테고리 코드
    private static final String SUBWAY_STATION_CATEGORY = "SW8";

    // 카카오 로컬 API가 한 번에 주는 최대 결과 수. maxResults가 이보다 크면 여러 페이지로 나눠 호출해서 채움
    private static final int PAGE_SIZE = 15;

    private static final KakaoPlaceSearchResponse EMPTY_RESPONSE =
            new KakaoPlaceSearchResponse(List.of(), new KakaoPlaceSearchResponse.Meta(true));

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    /**
     * 역 이름으로 좌표 검색. 호출 실패·매칭 실패 시 빈 값 (예외를 던지지 않음)
     */
    public Optional<Coordinates> searchStationCoordinates(String stationName) {
        List<KakaoPlaceSearchResponse.Document> documents = searchPage("/v2/local/search/keyword.json", uriBuilder -> uriBuilder
                .queryParam("query", stationName + "역")
                .queryParam("category_group_code", SUBWAY_STATION_CATEGORY))
                .documents();

        if (documents.isEmpty()) {
            log.warn("[경고] 카카오 로컬 API에서 역을 찾지 못함 - station: {}", stationName);
            return Optional.empty();
        }

        KakaoPlaceSearchResponse.Document document = documents.get(0);
        return Optional.of(new Coordinates(Double.parseDouble(document.y()), Double.parseDouble(document.x())));
    }

    /**
     * 좌표 주변 가게 검색(키워드+카테고리). 음식점 세부 카테고리(한식/중식 등)처럼 키워드가 필요할 때 사용
     * sort는 "distance"(거리순) 또는 "accuracy"(정확도순). radius는 정렬 방식과 상관없이 항상 적용됨
     */
    public List<KakaoPlaceSearchResponse.Document> searchPlacesByKeyword(String keyword, String categoryGroupCode,
            Coordinates origin, int radiusMeters, int size, String sort) {
        return searchAllPages("/v2/local/search/keyword.json", size, uriBuilder -> uriBuilder
                .queryParam("query", keyword)
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", String.valueOf(origin.lng()))
                .queryParam("y", String.valueOf(origin.lat()))
                .queryParam("radius", String.valueOf(radiusMeters))
                .queryParam("sort", sort));
    }

    /**
     * 좌표 주변 가게 검색(카테고리만). 카페처럼 세부 키워드가 필요 없을 때 사용
     */
    public List<KakaoPlaceSearchResponse.Document> searchPlacesByCategory(String categoryGroupCode, Coordinates origin,
            int radiusMeters, int size) {
        return searchAllPages("/v2/local/search/category.json", size, uriBuilder -> uriBuilder
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", String.valueOf(origin.lng()))
                .queryParam("y", String.valueOf(origin.lat()))
                .queryParam("radius", String.valueOf(radiusMeters))
                .queryParam("sort", "distance"));
    }

    // size가 한 페이지 최대치(PAGE_SIZE)보다 크면, 카카오가 "끝"이라고 할 때까지 페이지를 넘겨가며 이어붙임.
    // 카카오 페이지네이션은 (page, size) 조합이 그대로 "몇 번째~몇 번째" 구간 주소라서, 페이지마다 size를
    // 다르게 보내면 이미 받은 구간과 겹치는 엉뚱한 구간을 받아오게 됨 — 그래서 요청 size는 항상 PAGE_SIZE로
    // 고정해서 보내고, 다 모은 뒤 마지막에만 원래 요청받은 size만큼 잘라냄
    private List<KakaoPlaceSearchResponse.Document> searchAllPages(String path, int size,
            UnaryOperator<UriBuilder> queryParams) {
        List<KakaoPlaceSearchResponse.Document> results = new ArrayList<>();
        int page = 1;
        while (results.size() < size) {
            int currentPage = page;
            KakaoPlaceSearchResponse response = searchPage(path, uriBuilder -> queryParams.apply(uriBuilder)
                    .queryParam("size", String.valueOf(PAGE_SIZE))
                    .queryParam("page", String.valueOf(currentPage)));
            if (response.documents().isEmpty()) {
                break;
            }
            results.addAll(response.documents());
            if (response.meta().isEnd()) {
                break;
            }
            page++;
        }
        return results.size() > size ? results.subList(0, size) : results;
    }

    // 카카오 로컬 API 한 페이지 호출. 실패하면 빈 응답(끝난 것으로 취급)을 반환해서 호출부가 예외 처리를 신경 쓰지 않게 함
    private KakaoPlaceSearchResponse searchPage(String path, UnaryOperator<UriBuilder> queryParams) {
        String apiKey = properties.kakao().restApiKey();
        try {
            KakaoPlaceSearchResponse response = restClient.get()
                    .uri(uriBuilder -> queryParams.apply(uriBuilder.path(path)).build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);
            if (response == null) {
                return EMPTY_RESPONSE;
            }
            List<KakaoPlaceSearchResponse.Document> documents = response.documents() == null ? List.of() : response.documents();
            KakaoPlaceSearchResponse.Meta meta = response.meta() == null ? new KakaoPlaceSearchResponse.Meta(true) : response.meta();
            return new KakaoPlaceSearchResponse(documents, meta);
        } catch (Exception e) {
            log.warn("[경고] 카카오 로컬 API 호출 실패 - path: {}", path, e);
            return EMPTY_RESPONSE;
        }
    }

    // 위도(lat)·경도(lng) 한 쌍
    public record Coordinates(double lat, double lng) {
    }
}
