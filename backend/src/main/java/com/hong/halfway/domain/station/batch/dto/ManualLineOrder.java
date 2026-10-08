package com.hong.halfway.domain.station.batch.dto;

import java.util.List;

/**
 * 공공데이터로 못 채운 노선의 수동 입력 역 순서 (resources/station-order/manual-lines.json)
 */
public record ManualLineOrder(String line, List<StationEntry> stations) {

    public record StationEntry(String name, Double distanceKm, Integer minutes) {
    }
}
