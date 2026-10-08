package com.hong.halfway.domain.settlement.entity;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.room.entity.Room;
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
 * 방 1개당 1개, 정산을 먼저 시작(선점)한 결제자와 계좌 정보를 가짐
 */
@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    // 결제자로 선점한 참여자. 선착순이며 이후 바뀌지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_participant_id", nullable = false)
    private Participant payer;

    // 결제자 은행명. 토스 송금 딥링크 자동입력에 사용, 입력 전엔 null
    @Column(name = "payer_bank", length = 50)
    private String payerBank;

    // 결제자 계좌번호. 입력 전엔 null
    @Column(name = "payer_account_number", length = 50)
    private String payerAccountNumber;

    private Settlement(Room room, Participant payer) {
        this.room = room;
        this.payer = payer;
    }

    /**
     * 정산 시작 (결제자 선점). 중복 시작 방지는 서비스 레이어(+DB 유니크 제약)에서 확인
     */
    public static Settlement start(Room room, Participant payer) {
        return new Settlement(room, payer);
    }

    /**
     * 결제자 계좌 정보 입력·수정
     */
    public void updateAccount(String bank, String accountNumber) {
        this.payerBank = bank;
        this.payerAccountNumber = accountNumber;
    }
}
