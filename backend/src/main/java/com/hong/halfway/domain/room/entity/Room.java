package com.hong.halfway.domain.room.entity;

import com.hong.halfway.domain.place.entity.PlaceCategory;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    // 내부 식별자. 외부엔 절대 노출하지 않음 (링크·조회는 code로만)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 링크에 쓰이는 추측 불가능한 랜덤 코드
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    // 방장. 장소·일시 확정 같은 방장 전용 액션의 권한 기준
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    // 방 이름
    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "fairness_type", nullable = false, length = 20)
    private FairnessType fairnessType;

    // 확정된 약속 날짜. 방장이 방 만들 때 직접 입력(선택), 안 정하면 null(날짜 미정). 나중에 방장이 수정 가능
    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    // 확정된 약속 장소 역 이름. 투표 후보 중 방장이 확정하기 전까지는 null
    @Column(name = "confirmed_station_name")
    private String confirmedStationName;

    // 확정된 약속 음식점 이름. 역 확정 후 방장이 음식점까지 고르기 전까지는 null
    @Column(name = "confirmed_food_place_name")
    private String confirmedFoodPlaceName;

    // 확정된 약속 카페 이름. 역 확정 후 방장이 카페까지 고르기 전까지는 null
    @Column(name = "confirmed_cafe_place_name")
    private String confirmedCafePlaceName;

    // 이 방에서 음식점 선택이 필요한지 여부 (FIXED 모드 등에서 음식점만/카페만 고를 때 사용)
    @Column(name = "includes_food", nullable = false)
    private boolean includesFood;

    // 이 방에서 카페 선택이 필요한지 여부
    @Column(name = "includes_cafe", nullable = false)
    private boolean includesCafe;

    private Room(String code, User owner, String name, RoomMode mode, FairnessType fairnessType,
            LocalDate meetingDate, boolean includesFood, boolean includesCafe) {
        this.code = code;
        this.owner = owner;
        this.name = name;
        this.mode = mode;
        this.fairnessType = fairnessType;
        this.meetingDate = meetingDate;
        this.includesFood = includesFood;
        this.includesCafe = includesCafe;
        this.status = RoomStatus.COLLECTING;
    }

    /**
     * 새 약속 방 생성. 생성 시점 상태는 항상 COLLECTING
     */
    public static Room create(String code, User owner, String name, RoomMode mode, FairnessType fairnessType,
            LocalDate meetingDate, boolean includesFood, boolean includesCafe) {
        return new Room(code, owner, name, mode, fairnessType, meetingDate, includesFood, includesCafe);
    }

    /**
     * FIXED 모드 방 생성. 투표 단계 없이 입력받은 역으로 바로 CONFIRMED 상태에서 시작
     */
    public static Room createFixed(String code, User owner, String name, FairnessType fairnessType,
            LocalDate meetingDate, boolean includesFood, boolean includesCafe, String stationName) {
        Room room = new Room(code, owner, name, RoomMode.FIXED, fairnessType, meetingDate,
                includesFood, includesCafe);
        room.confirmedStationName = stationName;
        room.status = RoomStatus.CONFIRMED;
        return room;
    }

    /**
     * 정산만 하기 모드 방 생성. 역·장소 확정 없이 바로 CONFIRMED 상태에서 시작 (confirmedStationName은 계속 null)
     */
    public static Room createSettlementOnly(String code, User owner, String name, LocalDate meetingDate) {
        Room room = new Room(code, owner, name, RoomMode.SETTLEMENT_ONLY, FairnessType.MAX_MIN, meetingDate,
                true, true);
        room.status = RoomStatus.CONFIRMED;
        return room;
    }

    /**
     * 공평 기준 변경 (방장만 호출, 권한 체크는 서비스 레이어에서)
     */
    public void changeFairnessType(FairnessType fairnessType) {
        this.fairnessType = fairnessType;
    }

    /**
     * 투표 시작 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void startVoting() {
        this.status = RoomStatus.VOTING;
    }

    /**
     * 투표 취소 후 출발지 수집 상태로 복귀 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void cancelVoting() {
        this.status = RoomStatus.COLLECTING;
    }

    /**
     * 후보 중 하나를 약속 장소로 확정 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void confirm(String stationName) {
        this.confirmedStationName = stationName;
        this.status = RoomStatus.CONFIRMED;
    }

    /**
     * 확정 취소 후 투표 상태로 복귀 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void unconfirm() {
        this.confirmedStationName = null;
        this.confirmedFoodPlaceName = null;
        this.confirmedCafePlaceName = null;
        this.status = RoomStatus.VOTING;
    }

    /**
     * 역 확정 후 카테고리별(음식점/카페) 가게까지 확정 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void choosePlace(PlaceCategory category, String placeName) {
        if (category == PlaceCategory.FOOD) {
            this.confirmedFoodPlaceName = placeName;
        } else {
            this.confirmedCafePlaceName = placeName;
        }
    }

    /**
     * FIXED 모드 방의 확정 역 수정 (상태·권한 선행조건 체크는 서비스 레이어에서)
     */
    public void changeFixedStation(String stationName) {
        this.confirmedStationName = stationName;
    }

    /**
     * 약속 날짜 변경 (권한 체크는 서비스 레이어에서). null로 바꾸면 다시 날짜 미정 상태로 돌아감
     */
    public void changeMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    /**
     * 카테고리별 가게 선택 취소, 카테고리 탐색으로 복귀 (상태 선행조건 체크는 서비스 레이어에서)
     */
    public void clearPlace(PlaceCategory category) {
        if (category == PlaceCategory.FOOD) {
            this.confirmedFoodPlaceName = null;
        } else {
            this.confirmedCafePlaceName = null;
        }
    }
}
