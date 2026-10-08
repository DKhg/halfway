package com.hong.halfway.domain.station.repository;

/**
 * 중간지점 계산에서 출발 노드 기준으로 도착역 이름별 소요시간을 조회할 때 쓰는 프로젝션.
 * 엔티티 대신 필요한 컬럼만 받아서 N+1 없이 한 번에 조회
 */
public interface TravelTimeByNameProjection {

    Long getFromStationId();

    String getToStationName();

    short getMinutes();
}
