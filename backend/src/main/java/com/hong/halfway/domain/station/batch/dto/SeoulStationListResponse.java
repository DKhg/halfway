package com.hong.halfway.domain.station.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서울 열린데이터광장 SearchSTNBySubwayLineInfo 응답
 */
public record SeoulStationListResponse(@JsonProperty("SearchSTNBySubwayLineInfo") Body body) {

    public record Body(
            @JsonProperty("list_total_count") int totalCount,
            List<Row> row) {
    }

    public record Row(
            @JsonProperty("STATION_NM") String name,
            @JsonProperty("LINE_NUM") String line,
            @JsonProperty("FR_CODE") String frCode) {
    }
}
