package com.hong.halfway.domain.settlement.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentRequest;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentRequest.Content;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentRequest.GenerationConfig;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentRequest.InlineData;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentRequest.Part;
import com.hong.halfway.domain.settlement.client.dto.GeminiGenerateContentResponse;
import com.hong.halfway.domain.settlement.dto.ReceiptItemDraft;
import com.hong.halfway.global.config.ExternalApiProperties;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gemini API로 영수증 사진에서 품목명·금액을 추출. 이미지는 이 호출 한 번에만 쓰고 서버에 저장하지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReceiptClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private static final String PROMPT = """
            이 영수증 사진에서 상호명(가게 이름)과 최종 결제 금액(합계)을 읽어서 JSON으로만 응답해.
            {"name": "상호명", "amount": 숫자} 형태고, amount는 원 단위 정수야(쉼표·원 기호 없이).
            영수증이 아니거나 상호명·금액을 읽을 수 없으면 {"name": null, "amount": null}로 응답해.
            """;

    private final ExternalApiProperties properties;
    private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();
    // Spring Boot 4 기본 웹 스타터는 Jackson 3(tools.jackson)을 쓰고 com.fasterxml ObjectMapper 빈을 등록해주지 않아서 직접 생성
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 영수증 이미지에서 상호명·합계 금액을 추출. 호출 실패·인식 실패 시 name이 null인 빈 결과 (예외를 던지지 않음)
     */
    public ReceiptItemDraft extractReceipt(byte[] imageBytes, String mimeType) {
        String apiKey = properties.gemini().apiKey();
        String model = properties.gemini().model();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                List.of(new Content(List.of(
                        new Part(PROMPT, null),
                        new Part(null, new InlineData(mimeType, base64Image))))),
                new GenerationConfig("application/json"));

        try {
            GeminiGenerateContentResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={apiKey}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);

            String json = extractText(response);
            if (json == null || json.isBlank()) {
                return new ReceiptItemDraft(null, null);
            }
            return objectMapper.readValue(json, ReceiptItemDraft.class);
        } catch (Exception e) {
            log.warn("[경고] Gemini 영수증 인식 실패", e);
            return new ReceiptItemDraft(null, null);
        }
    }

    // 응답에서 모델이 생성한 텍스트(JSON 문자열) 한 조각만 꺼냄
    private String extractText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiGenerateContentResponse.Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().get(0).text();
    }
}
