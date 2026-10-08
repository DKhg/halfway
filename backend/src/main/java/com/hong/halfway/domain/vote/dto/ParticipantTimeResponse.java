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
}
