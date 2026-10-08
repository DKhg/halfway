package com.hong.halfway.domain.vote.repository;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.vote.entity.StationCandidate;
import com.hong.halfway.domain.vote.entity.StationVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationVoteRepository extends JpaRepository<StationVote, Long> {

    // 후보 목록 조회 시 후보별 득표를 한 번에 불러올 때 사용
    List<StationVote> findAllByCandidateIn(List<StationCandidate> candidates);

    // 투표 취소로 후보를 지울 때 함께 정리
    List<StationVote> findAllByCandidate(StationCandidate candidate);

    // 체크·체크 해제 시 중복 여부 확인
    Optional<StationVote> findByCandidateAndParticipant(StationCandidate candidate, Participant participant);
}
