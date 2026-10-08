package com.hong.halfway.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthTokenUtilsTest {

    @Test
    void 토큰을_생성하면_매번_다른_값이_나온다() {
        String token1 = AuthTokenUtils.generate();
        String token2 = AuthTokenUtils.generate();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void 같은_토큰을_해시하면_항상_같은_값이_나온다() {
        String token = AuthTokenUtils.generate();

        String hash1 = AuthTokenUtils.hash(token);
        String hash2 = AuthTokenUtils.hash(token);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void 다른_토큰은_다른_해시값이_나온다() {
        String hash1 = AuthTokenUtils.hash(AuthTokenUtils.generate());
        String hash2 = AuthTokenUtils.hash(AuthTokenUtils.generate());

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
