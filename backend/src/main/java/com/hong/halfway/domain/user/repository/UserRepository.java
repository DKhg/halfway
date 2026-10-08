package com.hong.halfway.domain.user.repository;

import com.hong.halfway.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 인증 필터에서 Authorization 헤더의 토큰을 해시해 사용자 조회할 때 사용
    Optional<User> findByTokenHash(String tokenHash);
}
