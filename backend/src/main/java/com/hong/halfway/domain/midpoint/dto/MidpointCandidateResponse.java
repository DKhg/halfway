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
public class MidpointCandidateResponse {

    private final String stationName;

    private final int maxMinutes;

    private final int sumMinutes;

    private final int fairnessScore;

    private final List<ParticipantTimeResponse> times;

    // 계산 결과(MidpointCandidate)를 응답 DTO로 변환. 참여자 ID → 닉네임 매핑으로 표시용 이름을 채움
    public static MidpointCandidateResponse from(MidpointCandidate candidate, Map<Long, String> nicknameByParticipantId) {
        List<ParticipantTimeResponse> times = candidate.minutesByParticipantId().entrySet().stream()
                .map(entry -> ParticipantTimeResponse.builder()
                        .participantId(entry.getKey())
                        .nickname(nicknameByParticipantId.get(entry.getKey()))
                        .minutes(entry.getValue())
                        .build())
                .toList();

        return MidpointCandidateResponse.builder()
                .stationName(candidate.stationName())
                .maxMinutes(candidate.maxMinutes())
                .sumMinutes(candidate.sumMinutes())
                .fairnessScore(candidate.fairnessScore())
                .times(times)
                .build();
    }
}
