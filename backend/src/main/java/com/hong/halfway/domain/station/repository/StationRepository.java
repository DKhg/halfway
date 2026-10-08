package com.hong.halfway.domain.station.repository;

import com.hong.halfway.domain.station.entity.Station;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    // 배치 재실행 시 중복 적재 방지
    boolean existsByNameAndLine(String name, String line);

    Optional<Station> findByNameAndLine(String name, String line);

    // 자동완성용 전체 목록. 이름별로 묶어서 노선을 같이 보여주기 위해 이름순으로 전체 조회
    List<Station> findAllByOrderByName();

    // 중간지점 계산에서 참여자 출발역 이름 → 실제 노선 노드(들) 조회
    List<Station> findAllByName(String name);

    // 막차 시간표 배치 대상(서울교통공사 운영 노선, 역코드 있는 역만) 조회
    List<Station> findAllByLineInAndFrCodeIsNotNull(Collection<String> lines);

    // TAGO 막차 시간표 배치 대상(서울교통공사 외 노선, TAGO 역 ID 있는 역만) 조회
    List<Station> findAllByTagoStationIdIsNotNull();
}
