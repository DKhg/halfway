package com.hong.halfway.domain.station.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GtfsLineResolverTest {

    @Test
    void 방향_표기가_없으면_대상이_아니다() {
        assertThat(GtfsLineResolver.resolve("수인분당선")).isEmpty();
    }

    @Test
    void 매핑이_필요한_노선명을_우리_노선명으로_바꾼다() {
        var info = GtfsLineResolver.resolve("수인분당선<하행>").orElseThrow();
        assertThat(info.line()).isEqualTo("수인분당");
        assertThat(info.inoutTag()).isEqualTo((short) 2);
        assertThat(info.express()).isFalse();
    }

    @Test
    void 표기가_같은_노선은_그대로_대상이_된다() {
        var info = GtfsLineResolver.resolve("서해선<상행>").orElseThrow();
        assertThat(info.line()).isEqualTo("서해선");
        assertThat(info.inoutTag()).isEqualTo((short) 1);
    }

    @Test
    void 지류가_괄호로_붙어도_같은_노선으로_묶인다() {
        var 용산 = GtfsLineResolver.resolve("경의중앙선(용산)<하행>").orElseThrow();
        var 서울역 = GtfsLineResolver.resolve("경의중앙선(서울역)<하행>").orElseThrow();
        assertThat(용산.line()).isEqualTo("경의중앙");
        assertThat(서울역.line()).isEqualTo("경의중앙");
    }

    @Test
    void 구호선_급행_표기를_인식한다() {
        var local = GtfsLineResolver.resolve("서울9호선(일반)<하행>").orElseThrow();
        var express = GtfsLineResolver.resolve("서울9호선(급행)<하행>").orElseThrow();
        assertThat(local.line()).isEqualTo("9호선");
        assertThat(local.express()).isFalse();
        assertThat(express.line()).isEqualTo("9호선");
        assertThat(express.express()).isTrue();
    }

    @Test
    void 대상_노선이_아니면_빈_값이다() {
        assertThat(GtfsLineResolver.resolve("서울1호선(광운대-신창)<하행>")).isEmpty();
    }
}
