package com.hong.halfway.domain.settlement.entity;

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
 * 정산 항목을 같이 나눠 낸 참여자 (한 항목에 여러 명, UNIQUE(settlement_item_id, participant_id))
 */
@Entity
@Table(name = "settlement_item_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItemMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_item_id", nullable = false)
    private SettlementItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    private SettlementItemMember(SettlementItem item, Participant participant) {
        this.item = item;
        this.participant = participant;
    }

    /**
     * 항목에 멤버 추가
     */
    public static SettlementItemMember of(SettlementItem item, Participant participant) {
        return new SettlementItemMember(item, participant);
    }
}
