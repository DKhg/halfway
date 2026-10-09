package com.hong.halfway.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.halfway.global.error.ErrorCode;
import com.hong.halfway.global.error.ErrorResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    // 토큰 인증 필터 등록 + 익명 사용자 생성·API 문서만 인증 예외
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 익명 사용자 생성은 토큰이 없는 상태에서 호출하는 API라 인증 불필요
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 인증 실패 시 프로젝트 공통 에러 형식({code, message})으로 401 응답
    // ObjectMapper는 빈으로 주입받지 않고 직접 생성 (Security 설정은 웹 MVC 메시지 컨버터보다 먼저 초기화될 수 있어서 별도로 둠)
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, authException) -> {
            response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(ErrorCode.UNAUTHORIZED)));
        };
    }

    // 프론트 개발 서버·배포 도메인만 CORS 허용
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
