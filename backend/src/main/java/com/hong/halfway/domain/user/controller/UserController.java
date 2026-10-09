package com.hong.halfway.domain.user.controller;

import com.hong.halfway.domain.user.dto.HomeStationUpdateRequest;
import com.hong.halfway.domain.user.dto.PairingCodeRedeemRequest;
import com.hong.halfway.domain.user.dto.PairingCodeResponse;
import com.hong.halfway.domain.user.dto.UserCreateResponse;
import com.hong.halfway.domain.user.dto.UserMeResponse;
import com.hong.halfway.domain.user.entity.PairingCode;
import com.hong.halfway.domain.user.service.PairingCodeService;
import com.hong.halfway.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final PairingCodeService pairingCodeService;

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
     * 페어링 코드 발급 (iOS 홈 화면 PWA 설치 전, 지금 쓰던 세션에서 호출)
     */
    @PostMapping("/me/pairing-code")
    public ResponseEntity<PairingCodeResponse> issuePairingCode(@AuthenticationPrincipal UUID userId) {
        PairingCode pairingCode = pairingCodeService.issue(userId);
        return ResponseEntity.ok(PairingCodeResponse.builder()
                .code(pairingCode.getCode())
                .expiresAt(pairingCode.getExpiresAt())
                .build());
    }

    /**
     * 페어링 코드 교환 (인증 불필요 - 홈 화면 PWA 최초 실행 때는 아직 토큰이 없는 상태로 호출됨)
     */
    @PostMapping("/pairing-code/redeem")
    public ResponseEntity<UserCreateResponse> redeemPairingCode(@RequestBody PairingCodeRedeemRequest request,
            HttpServletRequest httpRequest) {
        String token = pairingCodeService.redeem(request.getCode(), resolveClientIp(httpRequest));
        return ResponseEntity.ok(UserCreateResponse.builder().token(token).build());
    }

    // nginx가 X-Real-IP로 실제 접속자 IP를 전달해줌, 없으면(로컬 등) 요청의 원래 주소 사용
    private String resolveClientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null ? realIp : request.getRemoteAddr();
    }
}
