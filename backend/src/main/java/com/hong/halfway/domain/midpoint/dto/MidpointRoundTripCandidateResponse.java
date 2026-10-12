package com.hong.halfway.domain.midpoint.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MidpointRoundTripCandidateResponse {

    private final String stationName;

    private final int fairnessScore;

    private final List<MidpointRoundTripTimeResponse> times;
}
