package com.hong.halfway.domain.participant.repository;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    // 중복 참여 방지용
    boolean existsByRoomAndUser(Room room, User user);

    // 내 참여 정보 조회·수정·삭제에 사용
    Optional<Participant> findByRoomAndUser(Room room, User user);

    // 방 상세 조회에서 참여자 목록을 보여줄 때 사용
    List<Participant> findAllByRoom(Room room);

    // 방 목록 카드에 참여자 수를 보여줄 때 사용
    long countByRoom(Room room);

    // 내 약속 목록에서 내가 참여한 방을 모을 때 사용
    List<Participant> findAllByUser(User user);
}
