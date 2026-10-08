package com.hong.halfway.domain.room.repository;

import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 코드 중복 여부 확인용
    boolean existsByCode(String code);

    // 링크의 code로 방 조회
    Optional<Room> findByCode(String code);

    // 내 약속 목록에서 내가 방장인 방을 모을 때 사용
    List<Room> findByOwnerOrderByCreatedAtDesc(User owner);
}
