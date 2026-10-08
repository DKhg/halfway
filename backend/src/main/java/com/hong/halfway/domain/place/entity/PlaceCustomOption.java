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
 * 참여자가 추천 5개 외에 직접 검색해서 추가한 가게 후보. 카카오 로컬 API는 장소 단건 재조회가 없어 추가 시점 정보를 스냅샷으로 저장
 * UNIQUE(room_id, category, place_id)
 */
@Entity
@Table(name = "place_custom_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCustomOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    // 카카오 로컬 API 장소 id. 추천 후보와 동일한 체계라 투표(PlaceVote) 집계도 그대로 재사용됨
    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(name = "place_name", nullable = false, length = 255)
    private String placeName;

    @Column(name = "road_address_name", length = 255)
    private String roadAddressName;

    @Column(name = "place_url", length = 255)
    private String placeUrl;

    @Column(name = "walk_minutes")
    private Integer walkMinutes;

    // 추가한 사람이 남긴 짧은 한줄평 (선택, 최대 20자)
    @Column(length = 20)
    private String comment;

    // 이 가게를 직접 추가한 참여자. 본인 또는 방장이 삭제할 수 있는 기준
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    private PlaceCustomOption(Room room, PlaceCategory category, String placeId, String placeName,
            String roadAddressName, String placeUrl, Integer walkMinutes, String comment, Participant participant) {
        this.room = room;
        this.category = category;
        this.placeId = placeId;
        this.placeName = placeName;
        this.roadAddressName = roadAddressName;
        this.placeUrl = placeUrl;
        this.walkMinutes = walkMinutes;
        this.comment = comment;
        this.participant = participant;
    }

    /**
     * 검색 결과를 가게 후보로 추가. 중복 추가 방지는 서비스 레이어(+DB 유니크 제약)에서 확인
     */
    public static PlaceCustomOption add(Room room, PlaceCategory category, String placeId, String placeName,
            String roadAddressName, String placeUrl, Integer walkMinutes, String comment, Participant participant) {
        return new PlaceCustomOption(room, category, placeId, placeName, roadAddressName, placeUrl, walkMinutes,
                comment, participant);
    }
}
