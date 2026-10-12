package com.hong.halfway.domain.vote.controller;

import com.hong.halfway.domain.vote.dto.FixedStationUpdateRequest;
import com.hong.halfway.domain.vote.dto.VoteResultResponse;
import com.hong.halfway.domain.vote.service.VoteService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}")
public class VoteController {

    private final VoteService voteService;

    /**
     * 투표 시작. 현재 중간지점 후보를 고정하고 방을 VOTING 상태로 전환 (방장만 가능)
     * considerHome=true면 귀가역까지 고려한 후보로 고정
     */
    @PostMapping("/voting")
    public ResponseEntity<Void> startVoting(@PathVariable String code,
            @RequestParam(defaultValue = "false") boolean considerHome, @AuthenticationPrincipal UUID requesterId) {
        voteService.startVoting(code, considerHome, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 투표 취소. 고정된 후보를 지우고 출발지 수집 상태로 복귀 (방장만 가능)
     */
    @DeleteMapping("/voting")
    public ResponseEntity<Void> cancelVoting(@PathVariable String code, @AuthenticationPrincipal UUID requesterId) {
        voteService.cancelVoting(code, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 후보 목록 + 득표 현황 조회
     */
    @GetMapping("/candidates")
    public ResponseEntity<VoteResultResponse> getCandidates(@PathVariable String code,
            @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(voteService.getCandidates(code, requesterId));
    }

    /**
     * 후보 체크 ("괜찮아요")
     */
    @PutMapping("/candidates/{candidateId}/vote")
    public ResponseEntity<Void> vote(@PathVariable String code, @PathVariable Long candidateId,
            @AuthenticationPrincipal UUID requesterId) {
        voteService.vote(code, candidateId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 후보 체크 해제
     */
    @DeleteMapping("/candidates/{candidateId}/vote")
    public ResponseEntity<Void> unvote(@PathVariable String code, @PathVariable Long candidateId,
            @AuthenticationPrincipal UUID requesterId) {
        voteService.unvote(code, candidateId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 후보를 약속 장소로 확정 (방장만 가능)
     */
    @PostMapping("/candidates/{candidateId}/confirm")
    public ResponseEntity<Void> confirmCandidate(@PathVariable String code, @PathVariable Long candidateId,
            @AuthenticationPrincipal UUID requesterId) {
        voteService.confirmCandidate(code, candidateId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 확정 취소 후 투표 상태로 복귀 (방장만 가능)
     */
    @DeleteMapping("/confirm")
    public ResponseEntity<Void> unconfirm(@PathVariable String code, @AuthenticationPrincipal UUID requesterId) {
        voteService.unconfirm(code, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * FIXED 모드 방의 확정 역 수정 (방장만 가능)
     */
    @PatchMapping("/fixed-station")
    public ResponseEntity<Void> updateFixedStation(@PathVariable String code,
            @Valid @RequestBody FixedStationUpdateRequest request, @AuthenticationPrincipal UUID requesterId) {
        voteService.updateFixedStation(code, request.getStationName(), requesterId);
        return ResponseEntity.noContent().build();
    }
}
