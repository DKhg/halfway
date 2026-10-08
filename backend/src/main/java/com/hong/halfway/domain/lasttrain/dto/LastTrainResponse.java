package com.hong.halfway.domain.lasttrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LastTrainResponse {

    // "23:47" 형식. 계산 못 했으면(unsupported 포함) null
    private final String lastDepartureTime;

    // 경로 중간에 시간표 데이터가 없는 노선(1~9호선 외)이 있어 아직 계산할 수 없음
    private final boolean unsupported;

    public static LastTrainResponse of(String lastDepartureTime) {
        return LastTrainResponse.builder().lastDepartureTime(lastDepartureTime).unsupported(false).build();
    }

    public static LastTrainResponse unsupported() {
        return LastTrainResponse.builder().lastDepartureTime(null).unsupported(true).build();
    }
}
