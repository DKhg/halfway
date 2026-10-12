package com.hong.halfway.domain.midpoint.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.room.entity.FairnessType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MidpointFairnessCalculatorTest {

    // docs/DOMAIN.md 2절 "정답 예시" 표 그대로 (지우=1L, 민수=2L, 서연=3L)
    private Map<String, Map<Long, Integer>> exampleData() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("을지로3가", times(24, 22, 25));
        data.put("왕십리", times(28, 30, 16));
        data.put("교대", times(8, 36, 19));
        return data;
    }

    private Map<Long, Integer> times(int jiwoo, int minsu, int seoyeon) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        map.put(1L, jiwoo);
        map.put(2L, minsu);
        map.put(3L, seoyeon);
        return map;
    }

    @Test
    void MAX_MIN_기준으로_을지로3가_왕십리_교대_순으로_정렬된다() {
        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(exampleData(), FairnessType.MAX_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName)
                .containsExactly("을지로3가", "왕십리", "교대");
    }

    @Test
    void SUM_MIN_기준으로_교대_을지로3가_왕십리_순으로_정렬된다() {
        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(exampleData(), FairnessType.SUM_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName)
                .containsExactly("교대", "을지로3가", "왕십리");
    }

    @Test
    void max_sum_공평도가_정답_예시와_일치한다() {
        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(exampleData(), FairnessType.MAX_MIN);

        MidpointCandidate eulji = findByName(ranked, "을지로3가");
        assertThat(eulji.maxMinutes()).isEqualTo(25);
        assertThat(eulji.sumMinutes()).isEqualTo(71);
        assertThat(eulji.fairnessScore()).isEqualTo(95);

        MidpointCandidate wangsimni = findByName(ranked, "왕십리");
        assertThat(wangsimni.maxMinutes()).isEqualTo(30);
        assertThat(wangsimni.sumMinutes()).isEqualTo(74);
        assertThat(wangsimni.fairnessScore()).isEqualTo(75);

        MidpointCandidate gyodae = findByName(ranked, "교대");
        assertThat(gyodae.maxMinutes()).isEqualTo(36);
        assertThat(gyodae.sumMinutes()).isEqualTo(63);
        assertThat(gyodae.fairnessScore()).isEqualTo(45);
    }

    @Test
    void 한_명이라도_90분_초과면_후보에서_제외된다() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("너무먼역", times(10, 10, 95));
        data.put("가까운역", times(10, 10, 20));

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(data, FairnessType.MAX_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName).containsExactly("가까운역");
    }

    @Test
    void SUM_MIN은_합계와_최장시간이_같으면_공평도와_상관없이_순서를_그대로_유지한다() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("공평도낮은역", times(20, 20, 10)); // 합계 50, 최장 20, 공평도 72
        data.put("공평도높은역", times(20, 15, 15)); // 합계 50, 최장 20, 공평도 86 (더 높음)

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(data, FairnessType.SUM_MIN);

        // 공평도가 높은 쪽이 앞으로 오면 안 됨 (SUM_MIN은 공평도를 정렬에 안 씀)
        assertThat(ranked).extracting(MidpointCandidate::stationName)
                .containsExactly("공평도낮은역", "공평도높은역");
    }

    @Test
    void 소요시간이_모두_같으면_공평도는_100이다() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("완전공평역", times(15, 15, 15));

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(data, FairnessType.MAX_MIN);

        assertThat(ranked.get(0).fairnessScore()).isEqualTo(100);
    }

    @Test
    void rankRoundTrip도_MAX_MIN_기준으로_동일하게_정렬된다() {
        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rankRoundTrip(exampleData(), FairnessType.MAX_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName)
                .containsExactly("을지로3가", "왕십리", "교대");
    }

    @Test
    void rankRoundTrip은_90분을_넘어도_150분_이하면_후보에_남는다() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("왕복이라먼역", times(60, 60, 95));
        data.put("왕복도가까운역", times(10, 10, 20));

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rankRoundTrip(data, FairnessType.MAX_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName)
                .containsExactly("왕복도가까운역", "왕복이라먼역");
    }

    @Test
    void rankRoundTrip은_150분_초과면_후보에서_제외된다() {
        Map<String, Map<Long, Integer>> data = new LinkedHashMap<>();
        data.put("너무먼역", times(60, 60, 151));
        data.put("가까운역", times(10, 10, 20));

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rankRoundTrip(data, FairnessType.MAX_MIN);

        assertThat(ranked).extracting(MidpointCandidate::stationName).containsExactly("가까운역");
    }

    private MidpointCandidate findByName(List<MidpointCandidate> candidates, String name) {
        return candidates.stream()
                .filter(candidate -> candidate.stationName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
