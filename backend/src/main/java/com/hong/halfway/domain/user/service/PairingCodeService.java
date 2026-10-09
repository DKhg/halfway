package com.hong.halfway.domain.user.service;

import com.hong.halfway.domain.user.entity.PairingCode;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.PairingCodeRepository;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import com.hong.halfway.global.common.PairingCodeGenerator;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * iOS 홈 화면 PWA 설치 시 끊기는 익명 식별자를, 기존 브라우저 세션에서 발급한 4자리 코드로 이어주는 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PairingCodeService {

    // 코드 유효 시간
    private static final long EXPIRY_MINUTES = 15;

    // 코드 중복 시 재생성 최대 횟수 (10000가지 중 동시 활성 코드는 극소수라 실제 충돌은 거의 없음)
    private static final int CODE_GENERATION_MAX_ATTEMPTS = 5;

    private final PairingCodeRepository pairingCodeRepository;
    private final UserRepository userRepository;
    private final PairingCodeRateLimiter rateLimiter;

    /**
     * 페어링 코드 발급. 이미 쓸 수 있는(미사용·미만료) 코드가 있으면 재사용, 없으면 새로 발급
     */
    @Transactional
    public PairingCode issue(UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        return pairingCodeRepository
                .findFirstByUserAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userRepository.getReferenceById(userId), now)
                .orElseGet(() -> createNewCode(userId, now));
    }

    private PairingCode createNewCode(UUID userId, LocalDateTime now) {
        User user = userRepository.getReferenceById(userId);
        String code = generateUniqueCode(now);
        PairingCode pairingCode = pairingCodeRepository.save(
                PairingCode.issue(code, user, now.plusMinutes(EXPIRY_MINUTES)));
        log.info("[완료] 페어링 코드 발급 - userId: {}", userId);
        return pairingCode;
    }

    /**
     * 코드 교환. 유효하면 대상 사용자의 토큰을 새로 발급해서 반환 (기존 토큰은 무효화됨)
     */
    @Transactional
    public String redeem(String code, String requestIp) {
        if (!rateLimiter.tryAcquire(requestIp)) {
            throw new CustomException(ErrorCode.PAIRING_CODE_RATE_LIMITED);
        }

        LocalDateTime now = LocalDateTime.now();
        PairingCode pairingCode = pairingCodeRepository.findFirstByCodeOrderByCreatedAtDesc(code)
                .filter(found -> found.isUsable(now))
                .orElseThrow(() -> new CustomException(ErrorCode.PAIRING_CODE_INVALID));

        pairingCode.consume(now);

        String rawToken = AuthTokenUtils.generate();
        pairingCode.getUser().reissueTokenHash(AuthTokenUtils.hash(rawToken));
        log.info("[완료] 페어링 코드 교환 - userId: {}", pairingCode.getUser().getId());
        return rawToken;
    }

    // 현재 활성(미사용·미만료) 코드와 값이 안 겹치는 코드 생성
    private String generateUniqueCode(LocalDateTime now) {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_ATTEMPTS; attempt++) {
            String code = PairingCodeGenerator.generate();
            if (!pairingCodeRepository.existsByCodeAndUsedAtIsNullAndExpiresAtAfter(code, now)) {
                return code;
            }
        }
        log.error("[에러] 페어링 코드 생성 {}회 연속 충돌", CODE_GENERATION_MAX_ATTEMPTS);
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
