package com.hong.halfway.domain.midpoint.dto;

/**
 * 귀가역 포함 중간지점 계산 가능 여부
 */
public enum MidpointWithHomeStatus {

    // 출발역을 입력한 참여자가 2명 미만이라 아직 계산 불가
    WAITING,

    // 출발역 입력자 중 귀가역을 등록하지 않은 사람이 있어 계산 불가
    HOME_STATION_PENDING,

    // 계산 완료, candidates에 결과 있음
    READY,
}
