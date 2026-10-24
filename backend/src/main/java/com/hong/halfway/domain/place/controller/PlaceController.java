package com.hong.halfway.domain.place.controller;

import com.hong.halfway.domain.place.dto.PlaceConfirmRequest;
import com.hong.halfway.domain.place.dto.PlaceCustomAddRequest;
import com.hong.halfway.domain.place.dto.PlaceOptionResponse;
import com.hong.halfway.domain.place.entity.FoodSubCategory;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.service.PlaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}/places")
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 카테고리별 주변 장소 후보 + 득표 현황 조회. customOnly=true면 참여자가 직접 검색해서 추가한 곳만 ("직접추가" 탭).
     * radiusMeters는 참여자가 고른 추천 검색 반경(500/1000/2000만 허용), maxResults는 "더보기"로 지금까지 펼친 개수
     */
    @GetMapping
    public ResponseEntity<List<PlaceOptionResponse>> getPlaces(@PathVariable String code,
            @RequestParam PlaceCategory category, @RequestParam(required = false) FoodSubCategory subCategory,
            @RequestParam(defaultValue = "false") boolean customOnly,
            @RequestParam(defaultValue = "300") int radiusMeters,
            @RequestParam(defaultValue = "15") int maxResults, @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(placeService.getPlaces(code, category, subCategory, customOnly, radiusMeters, maxResults, requesterId));
    }

    /**
     * 자유 키워드로 가게 검색 (추천 목록과 별개, 직접 추가 전 미리보기)
     */
    @GetMapping("/search")
    public ResponseEntity<List<PlaceOptionResponse>> searchCustomPlaces(@PathVariable String code,
            @RequestParam PlaceCategory category, @RequestParam String keyword,
            @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(placeService.searchCustomPlaces(code, category, keyword, requesterId));
    }

    /**
     * 같은 역에서 다른 방이 이미 직접 추가했던 가게 후보 중 이 방에 아직 없는 것만 제안 ("다른 모임이 추가한 곳")
     */
    @GetMapping("/pool-suggestions")
    public ResponseEntity<List<PlaceOptionResponse>> getPoolSuggestions(@PathVariable String code,
            @RequestParam PlaceCategory category, @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(placeService.getPoolSuggestions(code, category, requesterId));
    }

    /**
     * 검색 결과를 가게 후보로 추가 (참여자면 누구나 가능)
     */
    @PostMapping("/custom")
    public ResponseEntity<Void> addCustomPlace(@PathVariable String code,
            @Valid @RequestBody PlaceCustomAddRequest request, @AuthenticationPrincipal UUID requesterId) {
        placeService.addCustomPlace(code, request, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 직접 추가한 가게 삭제 (본인이 추가한 것만 가능)
     */
    @DeleteMapping("/custom")
    public ResponseEntity<Void> deleteCustomPlace(@PathVariable String code, @RequestParam PlaceCategory category,
            @RequestParam String placeId, @AuthenticationPrincipal UUID requesterId) {
        placeService.deleteCustomPlace(code, category, placeId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 가게 후보 체크 ("괜찮아요")
     */
    @PutMapping("/vote")
    public ResponseEntity<Void> vote(@PathVariable String code, @RequestParam PlaceCategory category,
            @RequestParam String placeId, @AuthenticationPrincipal UUID requesterId) {
        placeService.vote(code, category, placeId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 가게 후보 체크 해제
     */
    @DeleteMapping("/vote")
    public ResponseEntity<Void> unvote(@PathVariable String code, @RequestParam PlaceCategory category,
            @RequestParam String placeId, @AuthenticationPrincipal UUID requesterId) {
        placeService.unvote(code, category, placeId, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카테고리별 가게 확정 (방장만 가능)
     */
    @PatchMapping("/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String code, @Valid @RequestBody PlaceConfirmRequest request,
            @AuthenticationPrincipal UUID requesterId) {
        placeService.confirmPlace(code, request.getCategory(), request.getPlaceName(), requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카테고리별 가게 선택 취소, 카테고리 탐색으로 복귀 (방장만 가능)
     */
    @DeleteMapping("/confirm")
    public ResponseEntity<Void> clear(@PathVariable String code, @RequestParam PlaceCategory category,
            @AuthenticationPrincipal UUID requesterId) {
        placeService.clearPlace(code, category, requesterId);
        return ResponseEntity.noContent().build();
    }
}
