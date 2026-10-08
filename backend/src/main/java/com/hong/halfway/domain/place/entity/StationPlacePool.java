package com.hong.halfway.domain.place.entity;

import com.hong.halfway.domain.user.entity.User;
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
 * 역별로 쌓이는 가게 후보 풀. 방이 삭제돼도 안 지워지며, 같은 역에서 모임을 여는 다음 방들에 제안용으로 노출
 * UNIQUE(station_name, category, place_id)
 */
@Entity
@Table(name = "station_place_pool")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationPlacePool extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 역 이름 (room.confirmedStationName과 동일 체계)
    @Column(name = "station_name", nullable = false, length = 50)
    private String stationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    // 카카오 로컬 API 장소 id
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

    // 최초로 추가한 사람이 남긴 짧은 한줄평 (선택, 최대 20자). 이후 같은 가게가 또 추가돼도 덮어쓰지 않음
    @Column(length = 20)
    private String comment;

    // 이 가게를 역 풀에 최초로 적재한 사용자 (정보 표시용, 삭제 권한과는 무관)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User addedByUser;

    private StationPlacePool(String stationName, PlaceCategory category, String placeId, String placeName,
            String roadAddressName, String placeUrl, Integer walkMinutes, String comment, User addedByUser) {
        this.stationName = stationName;
        this.category = category;
        this.placeId = placeId;
        this.placeName = placeName;
        this.roadAddressName = roadAddressName;
        this.placeUrl = placeUrl;
        this.walkMinutes = walkMinutes;
        this.comment = comment;
        this.addedByUser = addedByUser;
    }

    /**
     * 역 풀에 가게 후보 적재. 중복 추가 방지는 서비스 레이어(+DB 유니크 제약)에서 확인
     */
    public static StationPlacePool add(String stationName, PlaceCategory category, String placeId, String placeName,
            String roadAddressName, String placeUrl, Integer walkMinutes, String comment, User addedByUser) {
        return new StationPlacePool(stationName, category, placeId, placeName, roadAddressName, placeUrl, walkMinutes,
                comment, addedByUser);
    }
}
