package com.hong.halfway.domain.station.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 카카오 로컬 API "키워드로 장소 검색"/"카테고리로 장소 검색" 공통 응답
 */
public record KakaoPlaceSearchResponse(List<Document> documents, Meta meta) {

    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            @JsonProperty("road_address_name") String roadAddressName,
            @JsonProperty("place_url") String placeUrl,
            String distance,
            String x,
            String y) {
    }

    // isEnd=true면 다음 페이지가 없다는 뜻. 반경을 넓혔을 때 더 받아올 페이지가 있는지 판단하는 용도
    public record Meta(@JsonProperty("is_end") boolean isEnd) {
    }
}
