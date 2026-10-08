package com.hong.halfway.domain.station.repository;

import com.hong.halfway.domain.station.entity.StationTravelTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationTravelTimeRepository extends JpaRepository<StationTravelTime, Long> {

    // 참여자들의 출발 노드에서 갈 수 있는 모든 도착역 이름별 소요시간을 한 번에 조회 (중간지점 계산용)
    @Query("""
            SELECT stt.fromStation.id AS fromStationId, stt.toStation.name AS toStationName, stt.minutes AS minutes
            FROM StationTravelTime stt
            WHERE stt.fromStation.id IN :fromStationIds
            """)
    List<TravelTimeByNameProjection> findMinutesFromStations(@Param("fromStationIds") Collection<Long> fromStationIds);

    // 특정 역쌍의 최단 이동시간·경로 조회 (막차 계산에서 predecessor 체인을 한 칸씩 따라갈 때 사용)
    Optional<StationTravelTime> findByFromStation_IdAndToStation_Id(Long fromStationId, Long toStationId);

    // 참여자 출발역(S) 후보들과 귀가역(H) 후보들의 모든 조합 중 최단 쌍을 고르기 위한 조회 (막차 계산)
    List<StationTravelTime> findByFromStation_IdInAndToStation_IdIn(
            Collection<Long> fromStationIds, Collection<Long> toStationIds);
}
