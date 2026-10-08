package com.hong.halfway.domain.midpoint.service;

import com.hong.halfway.domain.room.entity.FairnessType;
import java.util.Comparator;
import java.util.Map;
import java.util.List;

/**
 * 중간지점 후보를 공평 기준으로 정렬·채점하는 순수 계산 로직 (docs/DOMAIN.md 2절 기준)
 */
public final class MidpointFairnessCalculator {

    // 한 명이라도 이 시간을 넘는 후보는 제외
    private static final int MAX_ACCEPTABLE_MINUTES = 90;

    // 최종 반환하는 후보 수
    private static final int TOP_CANDIDATE_COUNT = 3;

    private MidpointFairnessCalculator() {
    }

    /**
     * 후보별 참여자 소요시간을 받아 90분 초과 후보를 제외하고, 공평 기준에 따라 정렬해 상위 3개만 반환
     */
    public static List<MidpointCandidate> rank(Map<String, Map<Long, Integer>> minutesByCandidateAndParticipant,
            FairnessType fairnessType) {
        return minutesByCandidateAndParticipant.entrySet().stream()
                .map(entry -> toCandidate(entry.getKey(), entry.getValue()))
                .filter(candidate -> candidate.maxMinutes() <= MAX_ACCEPTABLE_MINUTES)
                .sorted(comparator(fairnessType))
                .limit(TOP_CANDIDATE_COUNT)
                .toList();
    }

    private static MidpointCandidate toCandidate(String stationName, Map<Long, Integer> minutesByParticipantId) {
        List<Integer> times = minutesByParticipantId.values().stream().toList();
        int max = times.stream().mapToInt(Integer::intValue).max().orElse(0);
        int sum = times.stream().mapToInt(Integer::intValue).sum();
        int fairnessScore = fairnessScore(times);
        return new MidpointCandidate(stationName, max, sum, fairnessScore, minutesByParticipantId);
    }

    // 공평도 = round(100 × (1 − 모표준편차 ÷ 평균)), 0 미만은 0, 모두 같으면 100
    private static int fairnessScore(List<Integer> times) {
        double mean = times.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (mean == 0) {
            return 100;
        }
        double variance = times.stream()
                .mapToDouble(time -> Math.pow(time - mean, 2))
                .average()
                .orElse(0);
        double stdev = Math.sqrt(variance);
        int score = (int) Math.round(100 * (1 - stdev / mean));
        return Math.max(score, 0);
    }

    // SUM_MIN은 편차를 신경 안 쓰는 기준이라 공평도를 정렬에 안 씀 (MAX_MIN만 마지막 tiebreak로 사용)
    private static Comparator<MidpointCandidate> comparator(FairnessType fairnessType) {
        Comparator<MidpointCandidate> maxAsc = Comparator.comparingInt(MidpointCandidate::maxMinutes);
        Comparator<MidpointCandidate> sumAsc = Comparator.comparingInt(MidpointCandidate::sumMinutes);
        Comparator<MidpointCandidate> fairnessDesc = Comparator.comparingInt(MidpointCandidate::fairnessScore).reversed();

        if (fairnessType == FairnessType.SUM_MIN) {
            return sumAsc.thenComparing(maxAsc);
        }
        return maxAsc.thenComparing(sumAsc).thenComparing(fairnessDesc);
    }
}
