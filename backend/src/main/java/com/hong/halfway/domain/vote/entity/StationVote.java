package com.hong.halfway.domain.vote.entity;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.global.common.BaseTimeEntity;
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
 * 참여자가 역 후보에 "괜찮아요" 체크한 기록 (한 참여자가 여러 후보 동시 체크 가능, UNIQUE(candidate_id, participant_id))
 */
@Entity
@Table(name = "station_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private StationCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    private StationVote(StationCandidate candidate, Participant participant) {
        this.candidate = candidate;
        this.participant = participant;
    }

    /**
     * 후보 체크. 중복 체크 방지는 서비스 레이어에서 확인
     */
    public static StationVote check(StationCandidate candidate, Participant participant) {
        return new StationVote(candidate, participant);
    }
}
