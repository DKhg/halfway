package com.hong.halfway.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoProfanityValidatorTest {

    private final NoProfanityValidator validator = new NoProfanityValidator();

    @Test
    void 평범한_닉네임은_통과한다() {
        assertThat(validator.isValid("홍길동", null)).isTrue();
    }

    @Test
    void 빈_값은_통과한다() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    void 욕설이_그대로_포함되면_막는다() {
        assertThat(validator.isValid("시발방", null)).isFalse();
        assertThat(validator.isValid("fuck you", null)).isFalse();
    }

    @Test
    void 공백이나_특수문자로_우회해도_막는다() {
        assertThat(validator.isValid("시.발", null)).isFalse();
        assertThat(validator.isValid("시 발", null)).isFalse();
        assertThat(validator.isValid("F-U-C-K", null)).isFalse();
    }
}
