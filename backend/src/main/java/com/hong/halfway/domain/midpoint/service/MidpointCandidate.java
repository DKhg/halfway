package com.hong.halfway.domain.midpoint.service;

import java.util.Map;

/**
 * 중간지점 후보 하나의 계산 결과. participantId → 소요시간(분)
 */
public record MidpointCandidate(String stationName, int maxMinutes, int sumMinutes, int fairnessScore,
        Map<Long, Integer> minutesByParticipantId) {
}
