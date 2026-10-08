package com.hong.halfway.domain.participant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ParticipantUpdateRequest {

    @NotBlank
    private String startStationName;

    // 안 정하면 출발역과 동일하게 처리 (docs/PRD.md: "비우면 출발역")
    private String homeStationName;
}
