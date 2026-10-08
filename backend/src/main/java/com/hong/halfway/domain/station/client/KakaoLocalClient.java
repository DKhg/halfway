package com.hong.halfway.domain.station.client;

import com.hong.halfway.domain.station.client.dto.KakaoPlaceSearchResponse;
import com.hong.halfway.global.config.ExternalApiProperties;
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

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

    /**
     * 역 이름으로 좌표 검색. 호출 실패·매칭 실패 시 빈 값 (예외를 던지지 않음)
     */
    public Optional<Coordinates> searchStationCoordinates(String stationName) {
        List<KakaoPlaceSearchResponse.Document> documents = search("/v2/local/search/keyword.json", uriBuilder -> uriBuilder
                .queryParam("query", stationName + "역")
                .queryParam("category_group_code", SUBWAY_STATION_CATEGORY));

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
        return search("/v2/local/search/keyword.json", uriBuilder -> uriBuilder
                .queryParam("query", keyword)
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", String.valueOf(origin.lng()))
                .queryParam("y", String.valueOf(origin.lat()))
                .queryParam("radius", String.valueOf(radiusMeters))
                .queryParam("size", String.valueOf(size))
                .queryParam("sort", sort));
    }

    /**
     * 좌표 주변 가게 검색(카테고리만). 카페처럼 세부 키워드가 필요 없을 때 사용
     */
    public List<KakaoPlaceSearchResponse.Document> searchPlacesByCategory(String categoryGroupCode, Coordinates origin,
            int radiusMeters, int size) {
        return search("/v2/local/search/category.json", uriBuilder -> uriBuilder
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", String.valueOf(origin.lng()))
                .queryParam("y", String.valueOf(origin.lat()))
                .queryParam("radius", String.valueOf(radiusMeters))
                .queryParam("size", String.valueOf(size))
                .queryParam("sort", "distance"));
    }

    // 카카오 로컬 API 공통 호출부. 실패하면 빈 목록을 반환해서 호출부가 예외 처리를 신경 쓰지 않게 함
    private List<KakaoPlaceSearchResponse.Document> search(String path, UnaryOperator<UriBuilder> queryParams) {
        String apiKey = properties.kakao().restApiKey();
        try {
            KakaoPlaceSearchResponse response = restClient.get()
                    .uri(uriBuilder -> queryParams.apply(uriBuilder.path(path)).build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);
            return response == null || response.documents() == null ? List.of() : response.documents();
        } catch (Exception e) {
            log.warn("[경고] 카카오 로컬 API 호출 실패 - path: {}", path, e);
            return List.of();
        }
    }

    // 위도(lat)·경도(lng) 한 쌍
    public record Coordinates(double lat, double lng) {
    }
}
