package com.hong.halfway.global.config;

import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer {token} 헤더로 익명 사용자 인증
 */
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    /**
     * 토큰이 유효하면 SecurityContext에 인증 정보 설정, 없거나 유효하지 않으면 그냥 통과
     * (permitAll 엔드포인트도 거쳐가는 필터라 여기서 막지 않고, 인증 필요 여부는 SecurityConfig가 판단)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extractToken(request)
                .map(AuthTokenUtils::hash)
                .flatMap(userRepository::findByTokenHash)
                .ifPresent(this::setAuthentication);

        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer {token} 헤더에서 토큰 원문 추출
    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    // principal을 User 엔티티가 아닌 UUID로 두어, 컨트롤러에서 @AuthenticationPrincipal UUID로 바로 받게 함
    private void setAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getId(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
