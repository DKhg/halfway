package com.hong.halfway.domain.station.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StationOptionResponse {

    private final String name;

    // 환승역은 노선이 여러 개라 리스트로 함께 내려줌
    private final List<String> lines;
}
