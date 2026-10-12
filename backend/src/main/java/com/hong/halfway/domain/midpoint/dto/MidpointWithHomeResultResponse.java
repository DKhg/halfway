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
public class MidpointWithHomeResultResponse {

    private final MidpointWithHomeStatus status;

    // 출발역을 입력한 참여자 수 (WAITING일 때 "몇 명 더 필요한지" 안내용)
    private final int readyParticipantCount;

    // 귀가역까지 등록한 사람 수 / 전체 (HOME_STATION_PENDING일 때 진행 상황 안내용)
    private final int homeStationSetCount;

    private final List<MidpointRoundTripCandidateResponse> candidates;

    public static MidpointWithHomeResultResponse waiting(int readyParticipantCount) {
        return MidpointWithHomeResultResponse.builder()
                .status(MidpointWithHomeStatus.WAITING)
                .readyParticipantCount(readyParticipantCount)
                .homeStationSetCount(0)
                .candidates(List.of())
                .build();
    }

    public static MidpointWithHomeResultResponse homeStationPending(int homeStationSetCount, int readyParticipantCount) {
        return MidpointWithHomeResultResponse.builder()
                .status(MidpointWithHomeStatus.HOME_STATION_PENDING)
                .readyParticipantCount(readyParticipantCount)
                .homeStationSetCount(homeStationSetCount)
                .candidates(List.of())
                .build();
    }

    // rankedCombined는 정렬·90분(여기선 150분) 필터용으로만 쓰고, 화면에는 출발·귀가 구간별 시간만 노출
    public static MidpointWithHomeResultResponse ready(List<MidpointCandidate> rankedCombined,
            Map<Long, Map<String, Integer>> departureMinutesByParticipantAndName,
            Map<Long, Map<String, Integer>> homeMinutesByParticipantAndName,
            Map<Long, String> nicknameByParticipantId) {
        List<MidpointRoundTripCandidateResponse> candidateResponses = rankedCombined.stream()
                .map(candidate -> {
                    List<MidpointRoundTripTimeResponse> times = nicknameByParticipantId.keySet().stream()
                            .map(participantId -> MidpointRoundTripTimeResponse.builder()
                                    .participantId(participantId)
                                    .nickname(nicknameByParticipantId.get(participantId))
                                    .departureMinutes(departureMinutesByParticipantAndName.get(participantId).get(candidate.stationName()))
                                    .homeMinutes(homeMinutesByParticipantAndName.get(participantId).get(candidate.stationName()))
                                    .build())
                            .toList();
                    return MidpointRoundTripCandidateResponse.builder()
                            .stationName(candidate.stationName())
                            .fairnessScore(candidate.fairnessScore())
                            .times(times)
                            .build();
                })
                .toList();

        return MidpointWithHomeResultResponse.builder()
                .status(MidpointWithHomeStatus.READY)
                .readyParticipantCount(nicknameByParticipantId.size())
                .homeStationSetCount(nicknameByParticipantId.size())
                .candidates(candidateResponses)
                .build();
    }
}
