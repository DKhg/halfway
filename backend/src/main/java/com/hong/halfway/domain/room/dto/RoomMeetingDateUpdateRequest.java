package com.hong.halfway.domain.room.dto;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomMeetingDateUpdateRequest {

    // null로 보내면 다시 날짜 미정 상태로 되돌림
    private LocalDate meetingDate;
}
