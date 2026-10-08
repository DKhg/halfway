package com.hong.halfway.domain.room.dto;

import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RoomCreateResponse {

    // 이후 방 조회·초대 링크에 쓰이는 코드
    private final String code;

    private final String name;

    private final RoomMode mode;

    private final FairnessType fairnessType;

    private final LocalDate meetingDate;

    private final RoomStatus status;

    // FIXED 모드로 즉시 확정된 경우에만 값이 채워짐
    private final String confirmedStationName;

    private final boolean includesFood;

    private final boolean includesCafe;

    // Room 엔티티를 응답 DTO로 변환
    public static RoomCreateResponse from(Room room) {
        return RoomCreateResponse.builder()
                .code(room.getCode())
                .name(room.getName())
                .mode(room.getMode())
                .fairnessType(room.getFairnessType())
                .meetingDate(room.getMeetingDate())
                .status(room.getStatus())
                .confirmedStationName(room.getConfirmedStationName())
                .includesFood(room.isIncludesFood())
                .includesCafe(room.isIncludesCafe())
                .build();
    }
}
