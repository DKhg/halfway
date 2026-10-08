package com.hong.halfway.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 익명_사용자를_생성하면_발급된_토큰으로_조회할_수_있다() {
        String rawToken = userService.createAnonymousUser();

        User found = userRepository.findByTokenHash(AuthTokenUtils.hash(rawToken)).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getOauthProvider()).isNull();
    }

    @Test
    void 호출할_때마다_다른_토큰이_발급된다() {
        String token1 = userService.createAnonymousUser();
        String token2 = userService.createAnonymousUser();

        assertThat(token1).isNotEqualTo(token2);
    }
}
