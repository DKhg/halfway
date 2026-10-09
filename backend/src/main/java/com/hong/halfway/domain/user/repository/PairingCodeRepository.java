package com.hong.halfway.domain.user.repository;

import com.hong.halfway.domain.user.entity.PairingCode;
import com.hong.halfway.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingCodeRepository extends JpaRepository<PairingCode, Long> {

    // 이미 발급된 재사용 가능한(미사용·미만료) 코드가 있는지 확인할 때 사용
    Optional<PairingCode> findFirstByUserAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            User user, LocalDateTime now);

    // 코드 발급 시 현재 활성(미사용·미만료) 코드와 값이 겹치는지 확인할 때 사용
    boolean existsByCodeAndUsedAtIsNullAndExpiresAtAfter(String code, LocalDateTime now);

    // 코드 교환(redeem) 시 조회. 같은 값이 여러 번 발급됐을 수 있어 가장 최근 것을 사용
    Optional<PairingCode> findFirstByCodeOrderByCreatedAtDesc(String code);
}
