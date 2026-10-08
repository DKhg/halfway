package com.hong.halfway.domain.room.dto;

import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RoomSummaryResponse {

    private final String code;

    private final String name;

    private final RoomMode mode;

    private final RoomStatus status;

    private final LocalDate meetingDate;

    private final boolean isOwner;

    private final long participantCount;

    // Room 엔티티를 내 약속 목록용 요약 DTO로 변환
    public static RoomSummaryResponse from(Room room, UUID requesterId, long participantCount) {
        return RoomSummaryResponse.builder()
                .code(room.getCode())
                .name(room.getName())
                .mode(room.getMode())
                .status(room.getStatus())
                .meetingDate(room.getMeetingDate())
                .isOwner(room.getOwner().getId().equals(requesterId))
                .participantCount(participantCount)
                .build();
    }
}
