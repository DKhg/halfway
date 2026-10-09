package com.hong.halfway.domain.settlement.client.dto;

import java.util.List;

/**
 * Gemini generateContent 응답 본문. 모델이 돌려준 텍스트(JSON 문자열)만 꺼내 쓰면 됨
 */
public record GeminiGenerateContentResponse(List<Candidate> candidates) {

    public record Candidate(Content content) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
