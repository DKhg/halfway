package com.hong.halfway.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * 욕설·비속어 포함 여부 검사. 공백·특수문자를 지우고 소문자로 바꾼 뒤 금칙어 목록과 부분일치로 비교.
 * 완벽한 필터는 아니고 기본적인 욕설만 거름 — 목록을 더 늘리거나 DB로 옮기는 건 추후 고려
 */
public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private static final Set<String> BANNED_WORDS = Set.of(
            "시발", "씨발", "씨팔", "시팔", "ㅅㅂ", "ㅆㅂ",
            "병신", "ㅄ", "지랄", "ㅈㄹ",
            "개새끼", "개새기", "씨댕", "씨댕이",
            "좆", "존나", "ㅈㄴ",
            "미친놈", "미친년",
            "걸레년", "창녀", "보지", "자지",
            "fuck", "fucker", "fucking", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "pussy");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.toLowerCase().replaceAll("[^0-9a-z가-힣ㄱ-ㅎㅏ-ㅣ]", "");
        return BANNED_WORDS.stream().noneMatch(normalized::contains);
    }
}
