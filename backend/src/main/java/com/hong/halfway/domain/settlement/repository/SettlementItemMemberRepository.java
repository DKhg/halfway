package com.hong.halfway.domain.settlement.repository;

import com.hong.halfway.domain.settlement.entity.SettlementItem;
import com.hong.halfway.domain.settlement.entity.SettlementItemMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemMemberRepository extends JpaRepository<SettlementItemMember, Long> {

    // 항목 목록 조회 시 항목별 멤버를 한 번에 불러올 때 사용
    List<SettlementItemMember> findAllByItemIn(List<SettlementItem> items);

    // 항목 수정 시 기존 멤버를 지우고 새로 등록
    void deleteAllByItem(SettlementItem item);

    // 항목 삭제 시 함께 정리
    void deleteAllByItemIn(List<SettlementItem> items);
}
