package com.hong.halfway.domain.participant.entity;

import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.user.entity.User;
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

@Entity
@Table(name = "participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // 이 참여가 어느 익명 사용자인지. UNIQUE(room_id, user_id)로 한 방에 중복 참여 방지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 이 방에서 쓸 닉네임. 욕설 필터는 아직 미적용
    @Column(nullable = false, length = 20)
    private String nickname;

    // 출발역 또는 주소. station 도메인이 아직 없어서 임시로 텍스트로 받음 (역 데이터 연결 전까지)
    @Column(name = "start_station_name")
    private String startStationName;

    private Participant(Room room, User user, String nickname) {
        this.room = room;
        this.user = user;
        this.nickname = nickname;
    }

    /**
     * 방 참여. 출발역 등 상세 정보는 이후 updateStartStation으로 입력
     */
    public static Participant join(Room room, User user, String nickname) {
        return new Participant(room, user, nickname);
    }

    /**
     * 출발역 입력 또는 수정
     */
    public void updateStartStation(String startStationName) {
        this.startStationName = startStationName;
    }
}
