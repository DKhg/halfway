package com.hong.halfway.domain.lasttrain.entity;

import com.hong.halfway.domain.station.entity.Station;
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
 * 역별 열차 시간표 원본 한 줄 (서울교통공사 1~9호선, 전량 재적재되는 배치 데이터)
 */
@Entity
@Table(name = "train_timetable")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainTimetable extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    // 열차 번호. 같은 열차의 다른 역 기록과 조인하면 실제 구간별 운행시간을 알 수 있음
    @Column(name = "train_no", nullable = false, length = 10)
    private String trainNo;

    // 요일타입 원본 코드 (1=평일, 2=토요일, 3=일요일·공휴일)
    @Column(name = "week_tag", nullable = false)
    private short weekTag;

    // 운행 방향 원본 코드. 노선마다 의미가 다름 (상행/하행 또는 내선/외선)
    @Column(name = "inout_tag", nullable = false)
    private short inoutTag;

    // 이 역 도착 시각(분). 이 역이 시발역이면 NULL. 자정 이후는 24시를 넘겨 표현
    @Column(name = "arrive_minute")
    private Short arriveMinute;

    // 이 역 출발 시각(분). 이 역이 종착역이면 NULL
    @Column(name = "leave_minute")
    private Short leaveMinute;

    // 이 열차의 시발역 이름
    @Column(name = "origin_station_name", nullable = false, length = 50)
    private String originStationName;

    // 이 열차의 종착역 이름
    @Column(name = "dest_station_name", nullable = false, length = 50)
    private String destStationName;

    // 급행 여부 원본 코드값
    @Column(name = "express_yn", length = 1)
    private String expressYn;

    private TrainTimetable(Station station, String trainNo, short weekTag, short inoutTag,
            Short arriveMinute, Short leaveMinute, String originStationName, String destStationName,
            String expressYn) {
        this.station = station;
        this.trainNo = trainNo;
        this.weekTag = weekTag;
        this.inoutTag = inoutTag;
        this.arriveMinute = arriveMinute;
        this.leaveMinute = leaveMinute;
        this.originStationName = originStationName;
        this.destStationName = destStationName;
        this.expressYn = expressYn;
    }

    /**
     * 공공데이터 배치 적재로 시간표 한 줄 생성
     */
    public static TrainTimetable create(Station station, String trainNo, short weekTag, short inoutTag,
            Short arriveMinute, Short leaveMinute, String originStationName, String destStationName,
            String expressYn) {
        return new TrainTimetable(station, trainNo, weekTag, inoutTag, arriveMinute, leaveMinute,
                originStationName, destStationName, expressYn);
    }
}
