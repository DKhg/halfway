package com.hong.halfway.domain.place.entity;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 참여자가 가게 후보(음식점/카페)에 "괜찮아요" 체크한 기록.
 * 한 참여자가 음식점·카페 각각 여러 곳 동시 체크 가능, UNIQUE(room_id, participant_id, category, place_id)
 */
@Entity
@Table(name = "place_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    // 체크 대상 가게의 대분류. 더미 데이터 단계라 가게 자체는 ID 없이 이름으로 식별
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    // 카카오 로컬 API 장소 id. 같은 상호가 여러 지점일 수 있어 이름 대신 id로 식별
    @Column(name = "place_id", nullable = false)
    private String placeId;

    private PlaceVote(Room room, Participant participant, PlaceCategory category, String placeId) {
        this.room = room;
        this.participant = participant;
        this.category = category;
        this.placeId = placeId;
    }

    /**
     * 가게 후보 체크. 중복 체크 방지는 서비스 레이어에서 확인
     */
    public static PlaceVote check(Room room, Participant participant, PlaceCategory category, String placeId) {
        return new PlaceVote(room, participant, category, placeId);
    }
}
