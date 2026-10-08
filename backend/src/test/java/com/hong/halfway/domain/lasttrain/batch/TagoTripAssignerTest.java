package com.hong.halfway.domain.lasttrain.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.lasttrain.batch.TagoTripAssigner.AssignedTrip;
import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagoTripAssignerTest {

    private Item item(String dep, String arr, String routeId, String end, String updown) {
        return new Item("S1", "테스트역", routeId, dep, arr, end, end == null ? null : end + "역", "01", updown);
    }

    @Test
    void 같은_종착역_그룹_안에서_출발시각_순서대로_순위가_매겨진다() {
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("054700", "054630", "R1", "E1", "U"),
                item("063200", "063130", "R1", "E1", "U"));

        List<AssignedTrip> assigned = TagoTripAssigner.assign(items);

        assertThat(assigned).hasSize(3);
        AssignedTrip first = assigned.stream().filter(a -> a.depTime().equals("054700")).findFirst().orElseThrow();
        AssignedTrip second = assigned.stream().filter(a -> a.depTime().equals("061900")).findFirst().orElseThrow();
        AssignedTrip third = assigned.stream().filter(a -> a.depTime().equals("063200")).findFirst().orElseThrow();
        assertThat(first.trainNo()).endsWith(":1");
        assertThat(second.trainNo()).endsWith(":2");
        assertThat(third.trainNo()).endsWith(":3");
        // 같은 그룹이면 전부 같은 trainNo 접두사(노선+방향+종착역)를 가져야 역간 조인이 성립함
        assertThat(first.trainNo()).isEqualTo(second.trainNo().replace(":2", ":1"));
    }

    @Test
    void 종착역이_다르면_순위가_따로_매겨진다() {
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("062200", "062130", "R1", "E2", "U"));

        List<AssignedTrip> assigned = TagoTripAssigner.assign(items);

        assertThat(assigned).hasSize(2);
        assertThat(assigned).allMatch(a -> a.trainNo().endsWith(":1"));
        assertThat(assigned.get(0).trainNo()).isNotEqualTo(assigned.get(1).trainNo());
    }

    @Test
    void 종착역_정보가_없으면_방향과_노선만으로_묶인다() {
        List<Item> items = List.of(
                item("000600", "0", "RGTX", null, "U"),
                item("002700", "0", "RGTX", null, "U"),
                item("233900", "0", "RGTX", null, "U"));

        List<AssignedTrip> assigned = TagoTripAssigner.assign(items);

        assertThat(assigned).hasSize(3);
        AssignedTrip last = assigned.stream().filter(a -> a.depTime().equals("233900")).findFirst().orElseThrow();
        assertThat(last.trainNo()).endsWith(":3");
        assertThat(last.destStationName()).isEqualTo("TAGO");
    }

    @Test
    void 방향이_다르면_같은_종착역이어도_다른_그룹이다() {
        List<Item> items = List.of(
                item("061900", "061830", "R1", "E1", "U"),
                item("231200", "231130", "R1", "E1", "D"));

        List<AssignedTrip> assigned = TagoTripAssigner.assign(items);

        assertThat(assigned).hasSize(2);
        assertThat(assigned).allMatch(a -> a.trainNo().endsWith(":1"));
        assertThat(assigned.get(0).trainNo()).isNotEqualTo(assigned.get(1).trainNo());
    }
}
