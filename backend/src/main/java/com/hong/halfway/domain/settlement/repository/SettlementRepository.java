package com.hong.halfway.domain.settlement.repository;

import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.settlement.entity.Settlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByRoom(Room room);

    boolean existsByRoom(Room room);
}
