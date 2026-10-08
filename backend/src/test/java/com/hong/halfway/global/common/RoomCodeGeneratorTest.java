package com.hong.halfway.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoomCodeGeneratorTest {

    @Test
    void 코드는_8자리다() {
        String code = RoomCodeGenerator.generate();

        assertThat(code).hasSize(8);
    }

    @Test
    void 호출할_때마다_다른_코드가_나온다() {
        String code1 = RoomCodeGenerator.generate();
        String code2 = RoomCodeGenerator.generate();

        assertThat(code1).isNotEqualTo(code2);
    }
}
