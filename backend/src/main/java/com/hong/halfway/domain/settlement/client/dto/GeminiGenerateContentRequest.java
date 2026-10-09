package com.hong.halfway.domain.settlement.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Gemini generateContent 요청 본문. 영수증 사진 한 장 + 프롬프트 텍스트 한 쌍만 보냄
 */
public record GeminiGenerateContentRequest(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
    }

    public record InlineData(String mimeType, String data) {
    }

    public record GenerationConfig(String responseMimeType) {
    }
}
