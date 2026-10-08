package com.hong.halfway.domain.lasttrain.dto;

/**
 * 막차 계산용 요일 구분. 날짜 투표가 아직 없어 사용자가 날짜 대신 이 중 하나를 직접 고름
 */
public enum LastTrainDayType {

    WEEKDAY((short) 1),
    SATURDAY((short) 2),
    HOLIDAY((short) 3);

    // train_timetable.week_tag 원본 코드값과 매핑
    private final short weekTag;

    LastTrainDayType(short weekTag) {
        this.weekTag = weekTag;
    }

    public short getWeekTag() {
        return weekTag;
    }
}
