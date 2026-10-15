package com.hong.halfway.domain.lasttrain.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.lasttrain.batch.dto.TagoTimetableResponse.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagoTimetableClientTest {

    private final TagoTimetableClient client = new TagoTimetableClient(null);

    private Item item(String dep, String arr) {
        return new Item("S1", "테스트역", "R1", dep, arr, "E1", "E1역", "02", "U");
    }

    @Test
    void 노선_방향_종착역_출발도착시각이_완전히_같으면_하나로_합친다() {
        List<Item> items = List.of(
                item("062600", "062600"),
                item("062600", "062600"),
                item("062600", "062600"));

        List<Item> result = client.dedupe(items);

        assertThat(result).hasSize(1);
    }

    @Test
    void 시각이_다르면_중복으로_보지_않는다() {
        List<Item> items = List.of(
                item("062600", "062600"),
                item("063300", "063300"));

        List<Item> result = client.dedupe(items);

        assertThat(result).hasSize(2);
    }
}
