package com.hong.halfway.domain.station.batch;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GTFS routes.txt의 route_long_name("경의중앙선(용산)&lt;하행&gt;", "서울9호선(급행)&lt;상행&gt;" 등)을
 * 우리 station.line 값 + 운행방향 + 급행여부로 해석. {@link com.hong.halfway.domain.lasttrain.batch.GtfsWeekdayTimetableBatchRunner}
 * (평일 시간표)와 {@link GtfsAdjacentTimeLoader}(역간 소요시간) 양쪽에서 같은 해석 규칙을 공유하기 위해 분리
 */
public final class GtfsLineResolver {

    // GTFS route_long_name의 노선 표기 -> 우리 station.line 값. 표기가 같은 노선(서해선·신림선·인천1·2호선·GTX-A)은 매핑 불필요
    private static final Map<String, String> LINE_ALIAS = Map.ofEntries(
            Map.entry("수인분당선", "수인분당"),
            Map.entry("경의중앙선", "경의중앙"),
            Map.entry("경춘선", "경춘"),
            Map.entry("신분당선", "신분당"),
            Map.entry("공항철도", "공항"),
            Map.entry("의정부경전철", "의정부"),
            Map.entry("용인경전철", "에버라인"),
            Map.entry("우이신설경전철", "우이신설"),
            Map.entry("경강선", "경강"),
            Map.entry("김포도시철도", "김포골드라인"),
            Map.entry("서울9호선", "9호선"));

    // LINE_ALIAS로 매핑하거나 표기가 그대로 같은 노선까지 포함한 전체 대상 노선(station.line 값 기준)
    private static final Set<String> TARGET_LINES = Set.of(
            "수인분당", "경의중앙", "경춘", "신분당", "공항", "의정부", "에버라인",
            "우이신설", "경강", "김포골드라인", "서해선", "신림선", "인천1호선", "인천2호선", "GTX-A", "9호선");

    private GtfsLineResolver() {
    }

    public record RouteInfo(String line, short inoutTag, boolean express) {
    }

    /**
     * route_long_name을 해석. 대상 노선이 아니거나 방향 표기(&lt;상행&gt;/&lt;하행&gt;)가 없으면 빈 값
     */
    public static Optional<RouteInfo> resolve(String longName) {
        short inoutTag;
        String base;
        if (longName.endsWith("<상행>")) {
            inoutTag = 1;
            base = longName.substring(0, longName.length() - 4);
        } else if (longName.endsWith("<하행>")) {
            inoutTag = 2;
            base = longName.substring(0, longName.length() - 4);
        } else {
            return Optional.empty();
        }

        // "경의중앙선(용산)", "GTX-A(수서-동탄)", "서울9호선(급행)"처럼 지류·급행 구분이 괄호로 붙은 경우 분리
        int parenIndex = base.indexOf('(');
        String lineKey = (parenIndex >= 0 ? base.substring(0, parenIndex) : base).trim();
        boolean express = parenIndex >= 0 && base.substring(parenIndex + 1, base.length() - 1).contains("급행");

        String dbLine = LINE_ALIAS.getOrDefault(lineKey, lineKey);
        if (!TARGET_LINES.contains(dbLine)) {
            return Optional.empty();
        }
        return Optional.of(new RouteInfo(dbLine, inoutTag, express));
    }
}
