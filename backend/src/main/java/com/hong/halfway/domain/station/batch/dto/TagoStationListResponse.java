package com.hong.halfway.domain.station.batch.dto;

import java.util.List;

/**
 * TAGO GetKwrdFndSubwaySttnList 응답
 */
public record TagoStationListResponse(Response response) {

    public record Response(Header header, Body body) {
    }

    public record Header(String resultCode, String resultMsg) {
    }

    public record Body(Items items, int numOfRows, int pageNo, int totalCount) {
    }

    public record Items(List<Item> item) {
    }

    public record Item(String subwayStationId, String subwayStationName, String subwayRouteName) {
    }
}
