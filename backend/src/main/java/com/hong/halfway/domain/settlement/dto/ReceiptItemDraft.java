package com.hong.halfway.domain.settlement.dto;

/**
 * 영수증 사진에서 Gemini가 추출한 상호명·합계 금액 초안. 참여자 배분은 아직 안 정해진 상태라 바로 저장하지 않고
 * 프론트에서 보여준 뒤 사용자가 확인·수정하면 기존 항목 추가 API로 저장함. 인식 실패 시 두 필드 모두 null
 */
public record ReceiptItemDraft(String name, Integer amount) {
}
