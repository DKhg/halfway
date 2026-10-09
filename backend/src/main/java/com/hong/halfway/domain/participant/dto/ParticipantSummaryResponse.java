package com.hong.halfway.domain.participant.dto;

import com.hong.halfway.domain.participant.entity.Participant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ParticipantSummaryResponse {

    private final Long id;

    private final String nickname;

    private final String startStationName;

    private final boolean isMe;

    // Participant 엔티티를 응답 DTO로 변환
    public static ParticipantSummaryResponse from(Participant participant, UUID requesterId) {
        boolean isMe = participant.getUser().getId().equals(requesterId);
        return ParticipantSummaryResponse.builder()
                .id(participant.getId())
                .nickname(participant.getNickname())
                .startStationName(participant.getStartStationName())
                .isMe(isMe)
                .build();
    }
}
