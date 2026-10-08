package com.hong.halfway.domain.station.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hong.halfway.domain.station.client.KakaoLocalClient;
import com.hong.halfway.domain.station.dto.StationOptionResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StationServiceTest {

    @Autowired
    private StationService stationService;

    @Autowired
    private StationRepository stationRepository;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    @Test
    void 환승역처럼_같은_이름이_여러_노선이어도_이름은_한_번만_나오고_노선은_함께_묶인다() {
        stationRepository.save(Station.create("테스트중앙역", "1호선테스트"));
        stationRepository.save(Station.create("테스트중앙역", "2호선테스트"));

        List<StationOptionResponse> options = stationService.getAllStationNames();

        List<StationOptionResponse> matched = options.stream()
                .filter(option -> option.getName().equals("테스트중앙역"))
                .toList();
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getLines()).containsExactlyInAnyOrder("1호선테스트", "2호선테스트");
    }

    @Test
    void 좌표가_없으면_카카오_API로_조회해서_채운다() {
        stationRepository.save(Station.create("좌표테스트역", "1호선테스트"));
        stationRepository.save(Station.create("좌표테스트역", "2호선테스트"));
        when(kakaoLocalClient.searchStationCoordinates("좌표테스트역"))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.1, 127.1)));

        stationService.ensureCoordinates("좌표테스트역");

        List<Station> stations = stationRepository.findAllByName("좌표테스트역");
        assertThat(stations).hasSize(2);
        assertThat(stations).allMatch(station -> station.getLat() == 37.1 && station.getLng() == 127.1);
    }

    @Test
    void 이미_좌표가_있으면_카카오_API를_호출하지_않는다() {
        Station station = Station.create("좌표있는역", "1호선테스트");
        station.updateCoordinates(37.2, 127.2);
        stationRepository.save(station);

        stationService.ensureCoordinates("좌표있는역");

        verify(kakaoLocalClient, never()).searchStationCoordinates(any());
    }

    @Test
    void 카카오_API가_못_찾으면_좌표는_null로_남는다() {
        stationRepository.save(Station.create("매칭실패역", "1호선테스트"));
        when(kakaoLocalClient.searchStationCoordinates("매칭실패역")).thenReturn(Optional.empty());

        stationService.ensureCoordinates("매칭실패역");

        List<Station> stations = stationRepository.findAllByName("매칭실패역");
        assertThat(stations).allMatch(station -> station.getLat() == null);
    }
}
