package com.hong.halfway.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserMeResponse {

    // 계정(익명 ID)에 저장된 귀가역. 안 정했으면 null
    private final String homeStationName;
}
