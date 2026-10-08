package com.hong.halfway.domain.vote.repository;

import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.vote.entity.StationCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationCandidateRepository extends JpaRepository<StationCandidate, Long> {

    // 투표 화면에서 후보를 순위대로 보여줄 때 사용
    List<StationCandidate> findAllByRoomOrderByRankAsc(Room room);
}
