package com.hong.halfway.domain.vote.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class VoteResultResponse {

    private final List<StationCandidateResponse> candidates;

    // 득표 1등 후보 ID. 동점이거나 아직 아무도 체크 안 했으면 null (방장 선택 대기)
    private final Long winnerCandidateId;
}
