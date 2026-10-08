package com.hong.halfway.domain.vote.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StationCandidateResponse {

    private final Long id;

    private final String stationName;

    private final int rank;

    private final int fairnessScore;

    private final int maxMinutes;

    private final int sumMinutes;

    private final List<ParticipantTimeResponse> times;

    private final int voteCount;

    // 내(요청자)가 이 후보를 체크했는지 여부
    private final boolean votedByMe;
}
