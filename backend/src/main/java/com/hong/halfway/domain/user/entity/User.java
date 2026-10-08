package com.hong.halfway.domain.user.entity;

import com.hong.halfway.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    // 기기별 익명 ID. 로그인 없이 이 UUID로 사용자를 구분
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 인증 토큰(평문)의 SHA-256 해시. 평문은 발급 시 한 번만 클라이언트에 내려주고 서버엔 저장하지 않음
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // 소셜 로그인 연동 시 provider 종류(KAKAO/GOOGLE/NAVER 등). 로그인 없이는 계속 null
    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    // 소셜 로그인 제공자 쪽 사용자 ID. oauthProvider와 한 쌍으로 유니크
    @Column(name = "oauth_id")
    private String oauthId;

    // 최근 참여 때 쓴 닉네임. 다음 방 참여 화면에 미리 채워주기 용도
    @Column(name = "last_nickname", length = 20)
    private String lastNickname;

    private User(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    /**
     * 기기별 익명 사용자 생성
     */
    public static User createAnonymous(String tokenHash) {
        return new User(tokenHash);
    }

    /**
     * 다음 방 참여 화면에 미리 채워줄 닉네임 갱신
     */
    public void updateLastNickname(String nickname) {
        this.lastNickname = nickname;
    }
}
