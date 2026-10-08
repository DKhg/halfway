package com.hong.halfway.domain.lasttrain.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서울 열린데이터광장 SearchSTNTimeTableByFRCodeService 응답
 */
public record SeoulTimetableResponse(@JsonProperty("SearchSTNTimeTableByFRCodeService") Body body) {

    public record Body(
            @JsonProperty("list_total_count") int totalCount,
            List<Row> row) {
    }

    public record Row(
            @JsonProperty("STATION_CD") String stationCd,
            @JsonProperty("TRAIN_NO") String trainNo,
            @JsonProperty("ARRIVETIME") String arriveTime,
            @JsonProperty("LEFTTIME") String leftTime,
            @JsonProperty("DESTSTATION") String destStation,
            @JsonProperty("SUBWAYSNAME") String originStationName,
            @JsonProperty("SUBWAYENAME") String destStationName,
            @JsonProperty("EXPRESS_YN") String expressYn) {
    }
}
