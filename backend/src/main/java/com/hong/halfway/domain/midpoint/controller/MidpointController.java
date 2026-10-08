package com.hong.halfway.domain.midpoint.controller;

import com.hong.halfway.domain.midpoint.dto.MidpointResultResponse;
import com.hong.halfway.domain.midpoint.service.MidpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}/midpoint")
public class MidpointController {

    private final MidpointService midpointService;

    /**
     * 중간지점 후보 계산 (참여자 출발역이 바뀔 때마다 새로 호출해서 재계산)
     */
    @GetMapping
    public ResponseEntity<MidpointResultResponse> getMidpoint(@PathVariable String code) {
        return ResponseEntity.ok(midpointService.getMidpointResult(code));
    }
}
