package com.hong.halfway.domain.lasttrain.repository;

/**
 * 막차 계산에서 한 구간(승차역 -> 하차역)을 실제로 이어주는 열차 조회용 프로젝션.
 * leaveMinute은 승차역 출발 시각, arriveMinute은 하차역 도착 시각
 */
public interface LatestTrainHopProjection {

    String getTrainNo();

    Short getLeaveMinute();

    Short getArriveMinute();
}
