package com.hong.halfway.domain.settlement.repository;

import com.hong.halfway.domain.settlement.entity.Settlement;
import com.hong.halfway.domain.settlement.entity.SettlementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    List<SettlementItem> findAllBySettlementOrderByIdAsc(Settlement settlement);
}
