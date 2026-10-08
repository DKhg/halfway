package com.hong.halfway.domain.settlement.entity;

import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
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
 * 정산 항목 하나 (예: "1차 고기·식사" 84,000원)
 */
@Entity
@Table(name = "settlement_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    // 항목 이름 (예: "1차 고기·식사")
    @Column(nullable = false, length = 50)
    private String name;

    // 항목 금액(원). 0보다 커야 함 (서비스 레이어에서 검증)
    @Column(nullable = false)
    private int amount;

    private SettlementItem(Settlement settlement, String name, int amount) {
        this.settlement = settlement;
        this.name = name;
        this.amount = amount;
    }

    /**
     * 정산 항목 추가
     */
    public static SettlementItem create(Settlement settlement, String name, int amount) {
        return new SettlementItem(settlement, name, amount);
    }

    /**
     * 항목 이름·금액 수정 (나눠 낼 멤버는 서비스 레이어에서 별도로 갱신)
     */
    public void update(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }
}
