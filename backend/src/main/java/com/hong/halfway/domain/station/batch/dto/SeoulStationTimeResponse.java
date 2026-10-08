package com.hong.halfway.domain.station.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서울 열린데이터광장 StationDstncReqreTimeHm 응답 (1~8호선 인접역 운행시간)
 */
public record SeoulStationTimeResponse(@JsonProperty("StationDstncReqreTimeHm") Body body) {

    public record Body(
            @JsonProperty("list_total_count") int totalCount,
            List<Row> row) {
    }

    public record Row(
            @JsonProperty("SBWY_ROUT_LN") String line,
            @JsonProperty("SBWY_STNS_NM") String stationName,
            // 이전 역에서 이 역까지 소요시간, "분:초" 형식 (예: "2:00")
            @JsonProperty("HM") String hm) {
    }
}
