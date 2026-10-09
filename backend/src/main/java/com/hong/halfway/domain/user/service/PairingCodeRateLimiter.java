package com.hong.halfway.domain.user.service;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * 페어링 코드 교환(redeem) 엔드포인트의 IP별 요청 횟수 제한. 인증 없이 호출되는 엔드포인트라
 * 무차별 대입(4자리, 10000가지)을 막기 위해 짧은 시간 안에 너무 많이 시도하면 거절함
 */
@Component
public class PairingCodeRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    /**
     * 이 IP가 제한 윈도우 안에서 허용 횟수를 넘었으면 false, 안 넘었으면 시도 1회로 기록하고 true
     */
    public boolean tryAcquire(String ip) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> attempts = attemptsByIp.computeIfAbsent(ip, key -> new ConcurrentLinkedDeque<>());

        synchronized (attempts) {
            while (!attempts.isEmpty() && now - attempts.peekFirst() > WINDOW_MILLIS) {
                attempts.pollFirst();
            }
            if (attempts.size() >= MAX_ATTEMPTS) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }
}
