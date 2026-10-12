package com.hong.halfway.domain.midpoint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 귀가역까지 고려한 후보에서, 참여자 한 명의 출발·귀가 구간별 소요시간 (합산 왕복시간은 의도적으로 안 보여줌)
@Getter
@AllArgsConstructor
@Builder
public class MidpointRoundTripTimeResponse {

    private final Long participantId;

    private final String nickname;

    private final int departureMinutes;

    private final int homeMinutes;
}
