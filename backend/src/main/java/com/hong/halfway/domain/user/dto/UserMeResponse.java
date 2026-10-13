package com.hong.halfway.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserMeResponse {

    // 계정(익명 ID)에 저장된 기본 닉네임. 방 참여 화면에 자동으로 채워주는 용도, 방마다 다르게 입력할 수도 있음. 안 정했으면 null
    private final String nickname;

    // 계정(익명 ID)에 저장된 귀가역. 안 정했으면 null
    private final String homeStationName;
}
