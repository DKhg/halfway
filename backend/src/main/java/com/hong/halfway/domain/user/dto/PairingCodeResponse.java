package com.hong.halfway.domain.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PairingCodeResponse {

    // 4자리 숫자 코드. iOS 홈 화면 추가 안내 화면에서 사용자에게 보여줌
    private final String code;

    private final LocalDateTime expiresAt;
}
