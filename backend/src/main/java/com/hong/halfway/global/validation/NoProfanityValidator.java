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
            "시발", "씨발", "씨팔", "시팔", "쉬발", "씨불", "ㅅㅂ", "ㅆㅂ", "ㅅㅂㄹㅁ",
            "병신", "병쉰", "ㅄ", "지랄", "ㅈㄹ", "염병", "ㅇㅄ",
            "개새끼", "개새기", "개쉐끼", "개자식", "씨댕", "씨댕이", "개소리",
            "좆", "좃", "존나", "존니", "ㅈㄴ", "씹", "씹새끼", "씹년", "씹할",
            "미친놈", "미친년", "미친새끼", "또라이", "정신병자",
            "걸레년", "걸레같", "창녀", "창년", "보지", "자지", "니미", "니애미", "느금마", "애미없", "애비없",
            "호로새끼", "호로자식", "잡년", "잡놈", "썅", "쌍년", "쌍놈",
            "뒤져", "뒈져", "강간", "변태새끼",
            "fuck", "fucker", "fucking", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "pussy",
            "motherfucker", "slut", "whore", "retard");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.toLowerCase().replaceAll("[^0-9a-z가-힣ㄱ-ㅎㅏ-ㅣ]", "");
        return BANNED_WORDS.stream().noneMatch(normalized::contains);
    }
}
