package com.hong.halfway.domain.station.batch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;

/**
 * 다익스트라로 한 역에서 다른 모든 역까지 최단 이동시간·환승횟수·경로(predecessor) 계산
 * 시간이 같으면 환승 적은 경로를 우선 (docs/DOMAIN.md 조건 필터 "환승 적게"와 일관되게)
 */
@Component
public class ShortestPathCalculator {

    // predecessorStationId는 이 역 바로 직전에 거친 역 (출발역 자신이면 null)
    public record Result(int totalSeconds, int transfers, Long predecessorStationId) {
    }

    private record State(Long stationId, int seconds, int transfers, Long predecessor) {
    }

    /**
     * source에서 도달 가능한 모든 역까지의 최단 이동시간·환승횟수·경로 계산
     */
    public Map<Long, Result> calculate(Long source, Map<Long, List<Edge>> graph) {
        Map<Long, Result> best = new HashMap<>();
        PriorityQueue<State> queue = new PriorityQueue<>(
                (a, b) -> a.seconds() != b.seconds() ? a.seconds() - b.seconds() : a.transfers() - b.transfers());

        queue.add(new State(source, 0, 0, null));
        while (!queue.isEmpty()) {
            State current = queue.poll();
            Result existing = best.get(current.stationId());
            if (existing != null && isBetterOrEqual(existing, current)) {
                continue;
            }
            best.put(current.stationId(), new Result(current.seconds(), current.transfers(), current.predecessor()));

            for (Edge edge : graph.getOrDefault(current.stationId(), List.of())) {
                int nextSeconds = current.seconds() + edge.seconds();
                int nextTransfers = current.transfers() + (edge.transfer() ? 1 : 0);
                Result nextBest = best.get(edge.targetStationId());
                if (nextBest == null || isBetter(nextSeconds, nextTransfers, nextBest)) {
                    queue.add(new State(edge.targetStationId(), nextSeconds, nextTransfers, current.stationId()));
                }
            }
        }

        best.remove(source);
        return best;
    }

    // 이미 확정된 결과가 현재 꺼낸 상태보다 같거나 나으면 스킵(중복 처리 방지)
    private boolean isBetterOrEqual(Result existing, State current) {
        return existing.totalSeconds() < current.seconds()
                || (existing.totalSeconds() == current.seconds() && existing.transfers() <= current.transfers());
    }

    // 새 후보가 지금까지의 최선 기록보다 나은지 (시간 우선, 같으면 환승 적은 쪽)
    private boolean isBetter(int seconds, int transfers, Result existing) {
        return seconds < existing.totalSeconds()
                || (seconds == existing.totalSeconds() && transfers < existing.transfers());
    }
}
