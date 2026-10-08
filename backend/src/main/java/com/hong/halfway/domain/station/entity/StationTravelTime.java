package com.hong.halfway.domain.station.entity;

import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 두 역 사이의 최단 이동시간. 다익스트라 계산 결과로만 채워지는 완전 파생 데이터
 * (배치 재실행 시 전체 삭제 후 재계산)
 */
@Entity
@Table(name = "station_travel_time")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationTravelTime extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    // 최단 이동시간 (분)
    @Column(nullable = false)
    private short minutes;

    // 최단 경로 상 환승 횟수
    @Column(nullable = false)
    private short transfers;

    // 이 역 바로 직전에 거친 역. 반복해서 따라가면 출발역까지의 전체 경유역 순서를 복원 가능
    // (막차 조회 시점에 환승역 후보 경로를 검증하는 용도)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_station_id")
    private Station predecessorStation;

    private StationTravelTime(Station fromStation, Station toStation, short minutes, short transfers,
            Station predecessorStation) {
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.minutes = minutes;
        this.transfers = transfers;
        this.predecessorStation = predecessorStation;
    }

    /**
     * 다익스트라 계산 결과로 최단 이동시간 레코드 생성 (predecessor 없음)
     */
    public static StationTravelTime create(Station fromStation, Station toStation, short minutes, short transfers) {
        return new StationTravelTime(fromStation, toStation, minutes, transfers, null);
    }

    /**
     * 다익스트라 계산 결과로 최단 이동시간 레코드 생성 (predecessor 포함, 배치 적재용)
     */
    public static StationTravelTime create(Station fromStation, Station toStation, short minutes, short transfers,
            Station predecessorStation) {
        return new StationTravelTime(fromStation, toStation, minutes, transfers, predecessorStation);
    }
}
