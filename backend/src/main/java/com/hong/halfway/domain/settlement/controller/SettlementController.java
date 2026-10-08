package com.hong.halfway.domain.settlement.controller;

import com.hong.halfway.domain.settlement.dto.SettlementAccountRequest;
import com.hong.halfway.domain.settlement.dto.SettlementItemRequest;
import com.hong.halfway.domain.settlement.dto.SettlementResponse;
import com.hong.halfway.domain.settlement.service.SettlementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 정산 시작 (결제자 선점, 선착순)
     */
    @PostMapping
    public ResponseEntity<Void> start(@PathVariable String code, @AuthenticationPrincipal UUID requesterId) {
        settlementService.start(code, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 취소. 결제자 선점과 입력된 항목을 모두 지우고 처음부터 다시 시작할 수 있게 함 (방장만 가능)
     */
    @DeleteMapping
    public ResponseEntity<Void> cancel(@PathVariable String code, @AuthenticationPrincipal UUID requesterId) {
        settlementService.cancel(code, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 항목·참여자별 총액 조회
     */
    @GetMapping
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String code,
            @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(settlementService.getSettlement(code, requesterId));
    }

    /**
     * 결제자 계좌 정보 입력·수정 (결제자만 가능)
     */
    @PatchMapping("/account")
    public ResponseEntity<Void> updateAccount(@PathVariable String code,
            @Valid @RequestBody SettlementAccountRequest request, @AuthenticationPrincipal UUID requesterId) {
        settlementService.updateAccount(code, request.getBank(), request.getAccountNumber(), requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 항목 추가 (결제자만 가능)
     */
    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@PathVariable String code, @Valid @RequestBody SettlementItemRequest request,
            @AuthenticationPrincipal UUID requesterId) {
        settlementService.addItem(code, request, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 항목 수정 (결제자만 가능)
     */
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<Void> updateItem(@PathVariable String code, @PathVariable Long itemId,
            @Valid @RequestBody SettlementItemRequest request, @AuthenticationPrincipal UUID requesterId) {
        settlementService.updateItem(code, itemId, request, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 항목 삭제 (결제자만 가능)
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String code, @PathVariable Long itemId,
            @AuthenticationPrincipal UUID requesterId) {
        settlementService.deleteItem(code, itemId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
