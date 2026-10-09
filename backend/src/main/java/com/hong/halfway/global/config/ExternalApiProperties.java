package com.hong.halfway.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공공데이터 API 인증키 설정 (application-local.yml에 실제 값 보관)
 */
@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(SeoulOpenData seoulOpenData, Tago tago, Kakao kakao, Gemini gemini) {

    public record SeoulOpenData(String generalKey, String subwayKey) {
    }

    public record Tago(String key) {
    }

    public record Kakao(String restApiKey) {
    }

    public record Gemini(String apiKey, String model) {
    }
}
