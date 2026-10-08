package com.hong.halfway.domain.station.batch;

/**
 * 그래프 간선. 운행 엣지(같은 노선 인접역)와 환승 엣지(같은 역, 다른 노선) 구분
 * 가중치는 초 단위 (분 단위로 누적하면 정차시간 같은 값을 더할 때 반올림 오차가 쌓여서 초 단위로 계산 후 최종 저장 때만 분으로 변환)
 */
public record Edge(Long targetStationId, int seconds, boolean transfer) {
}
