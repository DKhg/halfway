package com.hong.halfway.global.common;

import java.security.SecureRandom;

/**
 * 방·정산 링크에 쓰이는 추측 불가능한 랜덤 코드 생성
 */
public final class RoomCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RoomCodeGenerator() {
    }

    // 8자리 랜덤 코드 생성
    public static String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
