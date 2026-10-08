package com.hong.halfway.domain.user.service;

import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 기기별 익명 사용자 생성 및 인증 토큰 발급
     */
    @Transactional
    public String createAnonymousUser() {
        String rawToken = AuthTokenUtils.generate();
        String tokenHash = AuthTokenUtils.hash(rawToken);
        User user = userRepository.save(User.createAnonymous(tokenHash));
        log.info("[완료] 익명 사용자 생성 - ID: {}", user.getId());
        return rawToken;
    }
}
