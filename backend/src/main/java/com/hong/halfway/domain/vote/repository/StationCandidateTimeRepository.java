package com.hong.halfway.domain.vote.repository;

import com.hong.halfway.domain.vote.entity.StationCandidate;
import com.hong.halfway.domain.vote.entity.StationCandidateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationCandidateTimeRepository extends JpaRepository<StationCandidateTime, Long> {

    // 후보 목록 조회 시 참여자별 소요시간을 한 번에 불러올 때 사용
    List<StationCandidateTime> findAllByCandidateIn(List<StationCandidate> candidates);

    // 투표 취소로 후보를 지울 때 함께 정리
    List<StationCandidateTime> findAllByCandidate(StationCandidate candidate);
}
