package com.hong.halfway.domain.station.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.station.batch.dto.SeoulStationTimeResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StationGraphBuilderTest {

    @Autowired
    private StationGraphBuilder stationGraphBuilder;

    @Autowired
    private StationRepository stationRepository;

    private SeoulStationTimeResponse.Row row(String line, String stationName, String hm) {
        return new SeoulStationTimeResponse.Row(line, stationName, hm);
    }

    // 분기 경계 skip 목록은 실제 역명 기준이라 테스트도 실제 역명을 써야 하는데, 로컬 환경에 이미 같은 역이
    // 적재돼 있을 수 있어(배치를 미리 돌려둔 DB 등) 새로 만들지 않고 있으면 재사용
    private Station findOrCreate(String name, String line) {
        return stationRepository.findByNameAndLine(name, line).orElseGet(() -> stationRepository.save(Station.create(name, line)));
    }

    @Test
    void 분기_노선_경계에서는_API_응답_순서가_이어져도_가짜_지름길_엣지를_만들지_않는다() {
        // 실제 서울 열린데이터광장 5호선 응답 순서를 그대로 흉내냄: 하남지선 끝(하남검단산) 바로 다음 행이
        // 마천지선 시작(둔촌동)인데, 둘은 물리적으로 안 붙어있고 강동을 거쳐야 함
        Station hasimcheong = findOrCreate("하남시청", "5호선");
        Station hanam = findOrCreate("하남검단산", "5호선");
        Station dunchon = findOrCreate("둔촌동", "5호선");
        Station olympic = findOrCreate("올림픽공원", "5호선");

        List<SeoulStationTimeResponse.Row> rows = List.of(
                row("5", "하남시청", "1:50"),
                row("5", "하남검단산", "2:30"),
                row("5", "둔촌동", "1:50"),
                row("5", "올림픽공원", "1:40"));

        Map<Long, List<Edge>> graph = stationGraphBuilder.build(
                List.of(hasimcheong, hanam, dunchon, olympic), rows, List.of(),
                new GtfsAdjacentTimeLoader.Result(List.of(), Set.of()));

        boolean hasBogusShortcut = graph.get(hanam.getId()).stream()
                .anyMatch(edge -> edge.targetStationId().equals(dunchon.getId()));
        assertThat(hasBogusShortcut).isFalse();

        // 분기 경계가 아닌 정상 구간(하남시청-하남검단산, 둔촌동-올림픽공원)은 그대로 엣지가 생겨야 함
        boolean hasRealEdgeBeforeBranch = graph.get(hasimcheong.getId()).stream()
                .anyMatch(edge -> edge.targetStationId().equals(hanam.getId()));
        boolean hasRealEdgeAfterBranch = graph.get(dunchon.getId()).stream()
                .anyMatch(edge -> edge.targetStationId().equals(olympic.getId()));
        assertThat(hasRealEdgeBeforeBranch).isTrue();
        assertThat(hasRealEdgeAfterBranch).isTrue();
    }
}
