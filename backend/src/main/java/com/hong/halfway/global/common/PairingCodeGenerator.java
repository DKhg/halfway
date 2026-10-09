package com.hong.halfway.global.common;

import java.security.SecureRandom;

/**
 * 기기 간 익명 식별자 이어주기용 4자리 숫자 코드 생성
 */
public final class PairingCodeGenerator {

    private static final int EXCLUSIVE_UPPER_BOUND = 10000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PairingCodeGenerator() {
    }

    // 0000~9999 사이 4자리 숫자 코드 생성 (앞자리 0 유지)
    public static String generate() {
        return String.format("%04d", SECURE_RANDOM.nextInt(EXCLUSIVE_UPPER_BOUND));
    }
}
