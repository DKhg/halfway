package com.hong.halfway.domain.room.entity;

/**
 * 중간지점 후보 정렬 기준
 */
public enum FairnessType {

    // "모두 공평하게" (기본): 제일 오래 걸리는 사람 기준으로 최소화
    MAX_MIN,

    // "다 같이 빨리": 전체 이동시간 합을 최소화
    SUM_MIN,
}
