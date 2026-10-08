package com.hong.halfway.domain.lasttrain.repository;

import com.hong.halfway.domain.lasttrain.entity.TrainTimetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainTimetableRepository extends JpaRepository<TrainTimetable, Long> {

    // 배치 재실행 시, 이미 적재된 (역, 요일타입, 방향) 조합은 다시 호출하지 않고 건너뛰기 위함
    boolean existsByStationIdAndWeekTagAndInoutTag(Long stationId, short weekTag, short inoutTag);

    // 막차 계산 - 승차역 -> 하차역을 실제로 잇는 같은 열차(train_no)를 찾아, 하차 도착이 deadlineMinute 이전인 것 중
    // 승차 출발이 가장 늦은 순으로 정렬 (방향·노선이 다른 조합은 train_no가 같을 수 없어 자연히 걸러짐)
    @Query("""
            SELECT t1.trainNo AS trainNo, t1.leaveMinute AS leaveMinute, t2.arriveMinute AS arriveMinute
            FROM TrainTimetable t1, TrainTimetable t2
            WHERE t1.station.id = :boardStationId
              AND t2.station.id = :alightStationId
              AND t1.trainNo = t2.trainNo
              AND t1.weekTag = :weekTag
              AND t2.weekTag = :weekTag
              AND t1.leaveMinute IS NOT NULL
              AND t2.arriveMinute IS NOT NULL
              AND t1.leaveMinute < t2.arriveMinute
              AND t2.arriveMinute <= :deadlineMinute
            ORDER BY t1.leaveMinute DESC
            """)
    List<LatestTrainHopProjection> findHopsArrivingBefore(@Param("boardStationId") Long boardStationId,
            @Param("alightStationId") Long alightStationId, @Param("weekTag") short weekTag,
            @Param("deadlineMinute") int deadlineMinute);
}
