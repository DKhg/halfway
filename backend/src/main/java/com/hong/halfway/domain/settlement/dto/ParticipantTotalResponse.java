package com.hong.halfway.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ParticipantTotalResponse {

    private final Long participantId;

    private final String nickname;

    private final long amount;

    private final boolean payer;
}
