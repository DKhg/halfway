package com.hong.halfway.domain.midpoint.dto;

import com.hong.halfway.domain.midpoint.service.MidpointCandidate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MidpointResultResponse {

    private final MidpointStatus status;

    // 출발역을 입력한 참여자 수 (WAITING일 때 "몇 명 더 필요한지" 안내용)
    private final int readyParticipantCount;

    private final List<MidpointCandidateResponse> candidates;

    // 출발역 입력자가 2명 미만이라 아직 계산할 수 없는 상태
    public static MidpointResultResponse waiting(int readyParticipantCount) {
        return MidpointResultResponse.builder()
                .status(MidpointStatus.WAITING)
                .readyParticipantCount(readyParticipantCount)
                .candidates(List.of())
                .build();
    }

    // 계산 완료된 상위 후보 목록
    public static MidpointResultResponse ready(List<MidpointCandidate> candidates, Map<Long, String> nicknameByParticipantId) {
        List<MidpointCandidateResponse> candidateResponses = candidates.stream()
                .map(candidate -> MidpointCandidateResponse.from(candidate, nicknameByParticipantId))
                .toList();

        return MidpointResultResponse.builder()
                .status(MidpointStatus.READY)
                .readyParticipantCount(nicknameByParticipantId.size())
                .candidates(candidateResponses)
                .build();
    }
}
