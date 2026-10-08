package com.hong.halfway.domain.room.entity;

/**
 * 장소를 정하는 방식
 */
public enum RoomMode {

    // 참여자가 각자 출발역을 입력하면 중간지점을 자동 계산
    MIDPOINT,

    // 방장이 장소를 이미 정해서 만드는 방
    FIXED,

    // 추천·직접 추가한 후보 중에서 투표로 정함
    CANDIDATE,

    // 역·장소 확정 없이 정산만 빠르게 하는 방
    SETTLEMENT_ONLY,
}
