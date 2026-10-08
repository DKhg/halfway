package com.hong.halfway.domain.station.service;

import com.hong.halfway.domain.station.client.KakaoLocalClient;
import com.hong.halfway.domain.station.dto.StationOptionResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stationRepository;
    private final KakaoLocalClient kakaoLocalClient;

    /**
     * 이름별로 노선을 묶은 전체 역 목록 조회 (프론트 자동완성용, 배치 적재 시에만 바뀌는 데이터)
     */
    public List<StationOptionResponse> getAllStationNames() {
        List<Station> stations = stationRepository.findAllByOrderByName();

        Map<String, List<String>> linesByName = new LinkedHashMap<>();
        for (Station station : stations) {
            linesByName.computeIfAbsent(station.getName(), name -> new ArrayList<>()).add(station.getLine());
        }

        return linesByName.entrySet().stream()
                .map(entry -> new StationOptionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 역 좌표가 없으면 카카오 로컬 API로 조회해서 채움. 이미 있으면 호출 없이 그대로 둠 (이름이 같은 노선별 row 전체에 동일 좌표 저장)
     */
    @Transactional
    public void ensureCoordinates(String stationName) {
        List<Station> stations = stationRepository.findAllByName(stationName);
        if (stations.isEmpty()) {
            return;
        }
        boolean alreadyHasCoordinates = stations.stream()
                .anyMatch(station -> station.getLat() != null && station.getLng() != null);
        if (alreadyHasCoordinates) {
            return;
        }

        kakaoLocalClient.searchStationCoordinates(stationName).ifPresentOrElse(
                coordinates -> {
                    stations.forEach(station -> station.updateCoordinates(coordinates.lat(), coordinates.lng()));
                    log.info("[완료] 역 좌표 저장 - station: {}", stationName);
                },
                () -> log.warn("[경고] 역 좌표를 구하지 못함 - station: {}", stationName));
    }
}
