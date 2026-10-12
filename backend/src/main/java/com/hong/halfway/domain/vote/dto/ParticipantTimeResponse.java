package com.hong.halfway.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ParticipantTimeResponse {

    private final Long participantId;

    private final String nickname;

    private final int minutes;

    // 귀가역 고려 후보일 때만 값이 있음 (출발역 기준 후보는 null)
    private final Integer departureMinutes;

    // 귀가역 고려 후보일 때만 값이 있음 (출발역 기준 후보는 null)
    private final Integer homeMinutes;
}
