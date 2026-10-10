package com.hong.halfway.domain.lasttrain.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.AssignedTrip;
import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.StationItems;
import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse.Item;
import com.hong.halfway.domain.station.entity.Station;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TagoTripAssignerTest {

    private Item item(String dep, String arr, String routeId, String end, String updown) {
        return new Item("S1", "테스트역", routeId, dep, arr, end, end == null ? null : end + "역", "01", updown);
    }

    private Station station(String name) {
        return Station.create(name, "테스트선");
    }

    @Test
    void 역이_하나뿐이면_출발시각_순서대로_순위가_매겨진다() {
        Station a = station("A");
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("054700", "054630", "R1", "E1", "U"),
                item("063200", "063130", "R1", "E1", "U"));

        Map<Station, List<AssignedTrip>> result = TagoTripAssigner.assignAlongLine(List.of(new StationItems(a, items)));
        List<AssignedTrip> assigned = result.get(a);

        assertThat(assigned).hasSize(3);
        AssignedTrip first = assigned.stream().filter(x -> x.depTime().equals("054700")).findFirst().orElseThrow();
        AssignedTrip second = assigned.stream().filter(x -> x.depTime().equals("061900")).findFirst().orElseThrow();
        AssignedTrip third = assigned.stream().filter(x -> x.depTime().equals("063200")).findFirst().orElseThrow();
        assertThat(first.trainNo()).endsWith(":1");
        assertThat(second.trainNo()).endsWith(":2");
        assertThat(third.trainNo()).endsWith(":3");
        // 같은 그룹이면 전부 같은 trainNo 접두사(노선+방향+종착역)를 가져야 역간 조인이 성립함
        assertThat(first.trainNo()).isEqualTo(second.trainNo().replace(":2", ":1"));
    }

    @Test
    void 종착역이_다르면_순위가_따로_매겨진다() {
        Station a = station("A");
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("062200", "062130", "R1", "E2", "U"));

        List<AssignedTrip> assigned = TagoTripAssigner.assignAlongLine(List.of(new StationItems(a, items))).get(a);

        assertThat(assigned).hasSize(2);
        assertThat(assigned).allMatch(x -> x.trainNo().endsWith(":1"));
        assertThat(assigned.get(0).trainNo()).isNotEqualTo(assigned.get(1).trainNo());
    }

    @Test
    void 종착역_정보가_없으면_방향과_노선만으로_묶인다() {
        Station a = station("A");
        List<Item> items = List.of(
                item("000600", "0", "RGTX", null, "U"),
                item("002700", "0", "RGTX", null, "U"),
                item("233900", "0", "RGTX", null, "U"));

        List<AssignedTrip> assigned = TagoTripAssigner.assignAlongLine(List.of(new StationItems(a, items))).get(a);

        assertThat(assigned).hasSize(3);
        AssignedTrip last = assigned.stream().filter(x -> x.depTime().equals("233900")).findFirst().orElseThrow();
        assertThat(last.trainNo()).endsWith(":3");
        assertThat(last.destStationName()).isEqualTo("TAGO");
    }

    @Test
    void 방향이_다르면_같은_종착역이어도_다른_그룹이다() {
        Station a = station("A");
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("231200", "231130", "R1", "E1", "D"));

        List<AssignedTrip> assigned = TagoTripAssigner.assignAlongLine(List.of(new StationItems(a, items))).get(a);

        assertThat(assigned).hasSize(2);
        assertThat(assigned).allMatch(x -> x.trainNo().endsWith(":1"));
        assertThat(assigned.get(0).trainNo()).isNotEqualTo(assigned.get(1).trainNo());
    }

    @Test
    void 다음_역에서_이어지는_열차는_같은_식별자를_받는다() {
        Station a = station("A");
        Station b = station("B");
        List<Item> itemsA = List.of(
                item("061000", "060930", "R1", "E1", "U"),
                item("063000", "062930", "R1", "E1", "U"));
        // B는 A보다 10분 뒤 - 같은 두 열차가 그대로 이어짐
        List<Item> itemsB = List.of(
                item("062000", "061930", "R1", "E1", "U"),
                item("064000", "063930", "R1", "E1", "U"));

        Map<Station, List<AssignedTrip>> result = TagoTripAssigner.assignAlongLine(
                List.of(new StationItems(a, itemsA), new StationItems(b, itemsB)));

        String aFirst = result.get(a).stream().filter(x -> x.depTime().equals("061000")).findFirst().orElseThrow().trainNo();
        String aSecond = result.get(a).stream().filter(x -> x.depTime().equals("063000")).findFirst().orElseThrow().trainNo();
        String bFirst = result.get(b).stream().filter(x -> x.depTime().equals("062000")).findFirst().orElseThrow().trainNo();
        String bSecond = result.get(b).stream().filter(x -> x.depTime().equals("064000")).findFirst().orElseThrow().trainNo();

        assertThat(bFirst).isEqualTo(aFirst);
        assertThat(bSecond).isEqualTo(aSecond);
    }

    @Test
    void 중간역에서_새로_시작하는_열차가_있어도_뒤_역의_기존_열차_식별자가_밀리지_않는다() {
        // 왕십리~상봉(경의중앙선) 사례를 흉내냄: A->B 구간에서 B역에서 새로 출발하는 열차가 끼어들어도,
        // A에서 이미 타고 온 열차들의 식별자가 뒤로 밀리면 안 됨 (밀리면 역간 조인이 엉뚱한 열차끼리 묶임)
        Station a = station("A");
        Station b = station("B");
        Station c = station("C");
        List<Item> itemsA = List.of(
                item("061000", "060930", "R1", "E1", "U"),
                item("063000", "062930", "R1", "E1", "U"));
        // B: A의 06:10 열차가 이어짐(06:20) + B에서 새로 시작하는 열차(06:25) + A의 06:30 열차가 이어짐(06:40)
        List<Item> itemsB = List.of(
                item("062000", "061930", "R1", "E1", "U"),
                item("062500", "062430", "R1", "E1", "U"),
                item("064000", "063930", "R1", "E1", "U"));
        // C: 전부 B에서 10분 뒤로 그대로 이어짐
        List<Item> itemsC = List.of(
                item("063000", "062930", "R1", "E1", "U"),
                item("063500", "063430", "R1", "E1", "U"),
                item("065000", "064930", "R1", "E1", "U"));

        Map<Station, List<AssignedTrip>> result = TagoTripAssigner.assignAlongLine(
                List.of(new StationItems(a, itemsA), new StationItems(b, itemsB), new StationItems(c, itemsC)));

        String aAt0610 = result.get(a).stream().filter(x -> x.depTime().equals("061000")).findFirst().orElseThrow().trainNo();
        String aAt0630 = result.get(a).stream().filter(x -> x.depTime().equals("063000")).findFirst().orElseThrow().trainNo();

        String bAt0620 = result.get(b).stream().filter(x -> x.depTime().equals("062000")).findFirst().orElseThrow().trainNo();
        String bAt0625 = result.get(b).stream().filter(x -> x.depTime().equals("062500")).findFirst().orElseThrow().trainNo();
        String bAt0640 = result.get(b).stream().filter(x -> x.depTime().equals("064000")).findFirst().orElseThrow().trainNo();

        String cAt0630 = result.get(c).stream().filter(x -> x.depTime().equals("063000")).findFirst().orElseThrow().trainNo();
        String cAt0635 = result.get(c).stream().filter(x -> x.depTime().equals("063500")).findFirst().orElseThrow().trainNo();
        String cAt0650 = result.get(c).stream().filter(x -> x.depTime().equals("065000")).findFirst().orElseThrow().trainNo();

        // A에서 타고 온 두 열차는 B·C까지 같은 식별자를 유지해야 함
        assertThat(bAt0620).isEqualTo(aAt0610);
        assertThat(cAt0630).isEqualTo(aAt0610);
        assertThat(bAt0640).isEqualTo(aAt0630);
        assertThat(cAt0650).isEqualTo(aAt0630);
        // B에서 새로 시작한 열차는 A의 식별자와 겹치지 않는 새 식별자를 받고, C까지 그대로 이어져야 함
        assertThat(bAt0625).isNotEqualTo(aAt0610);
        assertThat(bAt0625).isNotEqualTo(aAt0630);
        assertThat(cAt0635).isEqualTo(bAt0625);
    }
}
