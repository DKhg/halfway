package com.hong.halfway.domain.room.entity;

/**
 * 방 진행 상태
 */
public enum RoomStatus {

    // 출발지 수집 중. 참여자가 입력할 때마다 결과 자동 재계산
    COLLECTING,

    // 후보가 고정되어 투표 중
    VOTING,

    // 날짜·장소가 확정됨
    CONFIRMED,

    // 약속이 끝나 더 이상 갱신되지 않음
    CLOSED,
}
