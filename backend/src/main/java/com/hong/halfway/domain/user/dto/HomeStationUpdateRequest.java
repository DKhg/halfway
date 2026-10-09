package com.hong.halfway.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HomeStationUpdateRequest {

    // 빈 값이면 귀가역 설정 해제
    private String homeStationName;
}
