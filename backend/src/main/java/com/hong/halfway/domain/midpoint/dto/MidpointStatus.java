package com.hong.halfway.domain.midpoint.dto;

/**
 * 중간지점 계산 가능 여부
 */
public enum MidpointStatus {

    // 출발역을 입력한 참여자가 2명 미만이라 아직 계산 불가
    WAITING,

    // 계산 완료, candidates에 결과 있음
    READY,
}
