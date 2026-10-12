package com.hong.halfway.domain.vote.entity;

import com.hong.halfway.domain.participant.entity.Participant;
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
 * 역 후보별 참여자 소요시간 스냅샷 (투표 시작 시점 고정, UNIQUE(candidate_id, participant_id))
 */
@Entity
@Table(name = "station_candidate_time")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationCandidateTime extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private StationCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    private short minutes;

    // 귀가역 고려 후보일 때만 채워짐: 출발역에서 이 후보까지 걸리는 시간(분)
    @Column(name = "departure_minutes")
    private Short departureMinutes;

    // 귀가역 고려 후보일 때만 채워짐: 이 후보에서 귀가역까지 걸리는 시간(분)
    @Column(name = "home_minutes")
    private Short homeMinutes;

    private StationCandidateTime(StationCandidate candidate, Participant participant, short minutes,
            Short departureMinutes, Short homeMinutes) {
        this.candidate = candidate;
        this.participant = participant;
        this.minutes = minutes;
        this.departureMinutes = departureMinutes;
        this.homeMinutes = homeMinutes;
    }

    /**
     * 투표 시작 시점 참여자별 소요시간 스냅샷 생성 (출발역 기준 후보)
     */
    public static StationCandidateTime snapshot(StationCandidate candidate, Participant participant, int minutes) {
        return new StationCandidateTime(candidate, participant, (short) minutes, null, null);
    }

    /**
     * 투표 시작 시점 참여자별 소요시간 스냅샷 생성 (귀가역 고려 후보 - 출발·귀가 구간을 따로 기록)
     */
    public static StationCandidateTime snapshotWithHome(StationCandidate candidate, Participant participant,
            int departureMinutes, int homeMinutes) {
        return new StationCandidateTime(candidate, participant, (short) (departureMinutes + homeMinutes),
                (short) departureMinutes, (short) homeMinutes);
    }
}
