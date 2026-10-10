package com.hong.halfway.domain.station.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.halfway.domain.station.batch.dto.ManualLineOrder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 수동 입력 노선 순서(station-order/manual-lines.json) 로드. 역 그래프 구성(StationGraphBuilder)과
 * TAGO 열차 식별(TagoTripAssigner)에서 같은 노선 순서 데이터를 공통으로 씀
 */
@Component
public class ManualLineOrderLoader {

    private static final String MANUAL_LINES_RESOURCE = "station-order/manual-lines.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ManualLineOrder> load() {
        try (InputStream is = new ClassPathResource(MANUAL_LINES_RESOURCE).getInputStream()) {
            return objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ManualLineOrder.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
