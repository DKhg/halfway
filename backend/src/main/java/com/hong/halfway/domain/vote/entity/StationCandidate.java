package com.hong.halfway.domain.vote.entity;

import com.hong.halfway.domain.room.entity.Room;
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
 * 투표 시작 시점에 고정된 중간지점 역 후보 스냅샷 (역 이름·순위·공평도·소요시간은 그 시점 값으로 고정)
 */
@Entity
@Table(name = "station_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    // 투표 시작 시점 순위 (1~3)
    @Column(nullable = false)
    private short rank;

    // 투표 시작 시점 공평도 스냅샷
    @Column(name = "fairness_score", nullable = false)
    private short fairnessScore;

    // 투표 시작 시점 최장 이동시간(분) 스냅샷
    @Column(name = "max_minutes", nullable = false)
    private short maxMinutes;

    // 투표 시작 시점 총 이동시간(분) 스냅샷
    @Column(name = "sum_minutes", nullable = false)
    private short sumMinutes;

    private StationCandidate(Room room, String stationName, short rank, short fairnessScore, short maxMinutes,
            short sumMinutes) {
        this.room = room;
        this.stationName = stationName;
        this.rank = rank;
        this.fairnessScore = fairnessScore;
        this.maxMinutes = maxMinutes;
        this.sumMinutes = sumMinutes;
    }

    /**
     * 투표 시작 시점 중간지점 계산 결과 하나를 후보 스냅샷으로 저장
     */
    public static StationCandidate snapshot(Room room, String stationName, int rank, int fairnessScore, int maxMinutes,
            int sumMinutes) {
        return new StationCandidate(room, stationName, (short) rank, (short) fairnessScore, (short) maxMinutes,
                (short) sumMinutes);
    }
}
