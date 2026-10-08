package com.hong.halfway.domain.user.controller;

import com.hong.halfway.domain.user.dto.UserCreateResponse;
import com.hong.halfway.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
