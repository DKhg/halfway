package com.hong.halfway.domain.user.dto;

import com.hong.halfway.global.validation.NoProfanity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NicknameUpdateRequest {

    @NotBlank
    @Size(max = 20)
    @NoProfanity
    private String nickname;
}
