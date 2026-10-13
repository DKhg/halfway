package com.hong.halfway.domain.user.controller;

import com.hong.halfway.domain.user.dto.HomeStationUpdateRequest;
import com.hong.halfway.domain.user.dto.NicknameUpdateRequest;
import com.hong.halfway.domain.user.dto.UserCreateResponse;
import com.hong.halfway.domain.user.dto.UserMeResponse;
import com.hong.halfway.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 익명 사용자 생성, 인증 토큰 발급 (인증 불필요)
     */
    @PostMapping
    public ResponseEntity<UserCreateResponse> createUser() {
        String token = userService.createAnonymousUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserCreateResponse.builder().token(token).build());
    }

    /**
     * 내 정보 조회 (계정에 저장된 귀가역)
     */
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    /**
     * 귀가역 설정·수정
     */
    @PatchMapping("/me/home-station")
    public ResponseEntity<Void> updateHomeStation(@RequestBody HomeStationUpdateRequest request,
            @AuthenticationPrincipal UUID userId) {
        userService.updateHomeStation(userId, request.getHomeStationName());
        return ResponseEntity.noContent().build();
    }

    /**
     * 기본 닉네임 설정·수정 (마이페이지)
     */
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@Valid @RequestBody NicknameUpdateRequest request,
            @AuthenticationPrincipal UUID userId) {
        userService.updateNickname(userId, request.getNickname());
        return ResponseEntity.noContent().build();
    }

    /**
     * 내 익명 계정 삭제 (데이터 초기화)
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UUID userId) {
        userService.deleteMyAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
