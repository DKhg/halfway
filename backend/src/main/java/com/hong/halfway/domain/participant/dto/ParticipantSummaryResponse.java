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

    // 요청자 본인일 때만 값이 채워짐 (다른 참여자에게는 막차 시각만 표시하는 게 원칙)
    private final String homeStationName;

    private final boolean isMe;

    // Participant 엔티티를 응답 DTO로 변환. 요청자 본인 항목에서만 귀가역을 노출
    public static ParticipantSummaryResponse from(Participant participant, UUID requesterId) {
        boolean isMe = participant.getUser().getId().equals(requesterId);
        return ParticipantSummaryResponse.builder()
                .id(participant.getId())
                .nickname(participant.getNickname())
                .startStationName(participant.getStartStationName())
                .homeStationName(isMe ? participant.getHomeStationName() : null)
                .isMe(isMe)
                .build();
    }
}
