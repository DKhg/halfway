package com.hong.halfway.domain.participant.dto;

import com.hong.halfway.domain.participant.entity.Participant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ParticipantResponse {

    private final Long id;

    private final String nickname;

    private final String startStationName;

    // Participant 엔티티를 응답 DTO로 변환
    public static ParticipantResponse from(Participant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .nickname(participant.getNickname())
                .startStationName(participant.getStartStationName())
                .build();
    }
}
