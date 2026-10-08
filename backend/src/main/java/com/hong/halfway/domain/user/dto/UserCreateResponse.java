package com.hong.halfway.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserCreateResponse {

    // 평문 토큰. 이 응답 한 번만 내려주고 서버엔 해시만 저장되므로 클라이언트가 직접 보관해야 함
    private final String token;
}
