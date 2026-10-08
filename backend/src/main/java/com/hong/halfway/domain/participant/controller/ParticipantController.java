package com.hong.halfway.domain.participant.controller;

import com.hong.halfway.domain.participant.dto.ParticipantJoinRequest;
import com.hong.halfway.domain.participant.dto.ParticipantResponse;
import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.service.ParticipantService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    /**
     * 방 참여 (닉네임만 입력)
     */
    @PostMapping
    public ResponseEntity<ParticipantResponse> join(@PathVariable String code, @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ParticipantJoinRequest request) {
        Participant participant = participantService.join(code, userId, request.getNickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(ParticipantResponse.from(participant));
    }

    /**
     * 출발역·귀가역 입력 또는 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<ParticipantResponse> updateMyInfo(@PathVariable String code,
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody ParticipantUpdateRequest request) {
        Participant participant = participantService.updateMyInfo(code, userId, request);
        return ResponseEntity.ok(ParticipantResponse.from(participant));
    }

    /**
     * 내 참여 정보 삭제
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> leave(@PathVariable String code, @AuthenticationPrincipal UUID userId) {
        participantService.leave(code, userId);
        return ResponseEntity.noContent().build();
    }
}
