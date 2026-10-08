package com.hong.halfway.domain.lasttrain.batch.dto;

import java.util.List;

/**
 * TAGO GetSubwaySttnAcctoSchdulList 응답. 서울교통공사 외 노선의 역별 시간표
 */
public record TagoTimetableResponse(Response response) {

    public record Response(Header header, Body body) {
    }

    public record Header(String resultCode, String resultMsg) {
    }

    public record Body(Items items, int numOfRows, int pageNo, int totalCount) {
    }

    public record Items(List<Item> item) {
    }

    /**
     * 역 하나의 열차 한 편 기록. arrTime은 "이 역" 도착 시각, depTime은 "이 역" 출발 시각(둘 다 HHMMSS).
     * endSubwayStationId/Nm(종착역)은 일부 노선(예: GTX-A)에선 응답에서 아예 빠지기도 함
     */
    public record Item(
            String subwayStationId,
            String subwayStationNm,
            String subwayRouteId,
            String depTime,
            String arrTime,
            String endSubwayStationId,
            String endSubwayStationNm,
            String dailyTypeCode,
            String upDownTypeCode) {
    }
}
