package com.hong.halfway.domain.station.entity;

import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "station")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 역 이름. 같은 이름이라도 노선이 다르면 별개 row (환승역은 노선 수만큼 row가 생김)
    @Column(nullable = false)
    private String name;

    // 노선명 (예: "2호선", "공항")
    @Column(nullable = false)
    private String line;

    // 좌표. 공공데이터 역 목록엔 없어서, 역이 실제로 확정되는 시점에 카카오 로컬 API로 채워짐
    private Double lat;

    private Double lng;

    // 중간지점 후보로 쓸 만한 역인지. 아직 선별 기준이 없어서 배치 적재 시 기본 true
    @Column(name = "is_meeting_spot", nullable = false)
    private boolean meetingSpot;

    // 서울교통공사 내부 역코드. 열차 시간표 API 조회용, 서울교통공사 운영 노선(1~9호선)만 값이 있음
    @Column(name = "fr_code", length = 10)
    private String frCode;

    // TAGO 지하철역 ID. 열차 시간표 API 조회용, 서울교통공사 외 노선 역만 값이 있음
    @Column(name = "tago_station_id", length = 20)
    private String tagoStationId;

    private Station(String name, String line) {
        this.name = name;
        this.line = line;
        this.meetingSpot = true;
    }

    /**
     * 공공데이터 배치 적재로 역 생성. 좌표는 이후 별도 보강
     */
    public static Station create(String name, String line) {
        return new Station(name, line);
    }

    /**
     * 카카오 로컬 API로 조회한 좌표 저장
     */
    public void updateCoordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * 서울교통공사 역코드 저장 (열차 시간표 API 조회용)
     */
    public void updateFrCode(String frCode) {
        this.frCode = frCode;
    }

    /**
     * TAGO 지하철역 ID 저장 (열차 시간표 API 조회용)
     */
    public void updateTagoStationId(String tagoStationId) {
        this.tagoStationId = tagoStationId;
    }
}
