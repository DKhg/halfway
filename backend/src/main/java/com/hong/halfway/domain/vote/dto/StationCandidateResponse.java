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

    // 귀가역까지 고려해서 고정한 후보인지 (true면 times의 departureMinutes/homeMinutes가 채워짐)
    private final boolean considerHome;

    private final List<ParticipantTimeResponse> times;

    private final int voteCount;

    // 내(요청자)가 이 후보를 체크했는지 여부
    private final boolean votedByMe;
}
