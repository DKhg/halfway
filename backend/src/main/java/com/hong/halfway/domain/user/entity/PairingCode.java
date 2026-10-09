package com.hong.halfway.domain.user.entity;

import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pairing_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 4자리 숫자 코드. 동시에 활성인 코드끼리만 안 겹치게 발급 시 애플리케이션에서 검사
    @Column(name = "code", nullable = false, length = 4)
    private String code;

    // 이 코드로 복원할 대상 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 만료 시각 (발급 후 15분)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 교환(사용) 성공한 시각. null이면 아직 미사용
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    private PairingCode(String code, User user, LocalDateTime expiresAt) {
        this.code = code;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    /**
     * 페어링 코드 발급
     */
    public static PairingCode issue(String code, User user, LocalDateTime expiresAt) {
        return new PairingCode(code, user, expiresAt);
    }

    /**
     * 지금 시점 기준으로 교환 가능한 상태인지 (미사용, 미만료)
     */
    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    /**
     * 교환 처리 (사용 시각 기록). 호출 전 isUsable 확인 필수
     */
    public void consume(LocalDateTime now) {
        this.usedAt = now;
    }
}
