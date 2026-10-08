package com.hong.halfway.domain.room.controller;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.dto.RoomCreateResponse;
import com.hong.halfway.domain.room.dto.RoomDetailResponse;
import com.hong.halfway.domain.room.dto.RoomMeetingDateUpdateRequest;
import com.hong.halfway.domain.room.dto.RoomSummaryResponse;
import com.hong.halfway.domain.room.dto.RoomUpdateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.settlement.service.SettlementService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ParticipantService participantService;
    private final SettlementService settlementService;

    /**
     * 방 생성 (인증 필요, 요청자가 방장이 됨)
     */
    @PostMapping
    public ResponseEntity<RoomCreateResponse> createRoom(@AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody RoomCreateRequest request) {
        Room room = roomService.createRoom(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomCreateResponse.from(room));
    }

    /**
     * 내가 방장이거나 참여한 방 목록 조회 (내 약속 목록용)
     */
    @GetMapping("/mine")
    public ResponseEntity<List<RoomSummaryResponse>> getMyRooms(@AuthenticationPrincipal UUID requesterId) {
        List<Room> rooms = roomService.getMyRooms(requesterId);
        List<RoomSummaryResponse> response = rooms.stream()
                .map(room -> RoomSummaryResponse.from(room, requesterId, participantService.countParticipants(room)))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 방 상세 조회 (참여자 목록 포함, 인증 필요)
     */
    @GetMapping("/{code}")
    public ResponseEntity<RoomDetailResponse> getRoom(@PathVariable String code,
            @AuthenticationPrincipal UUID requesterId) {
        Room room = roomService.getRoom(code);
        List<Participant> participants = participantService.getParticipants(code);
        boolean settlementStarted = settlementService.existsForRoom(room);
        return ResponseEntity.ok(RoomDetailResponse.from(room, participants, requesterId, settlementStarted));
    }

    /**
     * 방 설정 변경 (공평 기준, 방장만 가능)
     */
    @PatchMapping("/{code}")
    public ResponseEntity<RoomDetailResponse> updateRoom(@PathVariable String code,
            @AuthenticationPrincipal UUID requesterId, @Valid @RequestBody RoomUpdateRequest request) {
        Room room = roomService.updateRoom(code, requesterId, request);
        List<Participant> participants = participantService.getParticipants(code);
        boolean settlementStarted = settlementService.existsForRoom(room);
        return ResponseEntity.ok(RoomDetailResponse.from(room, participants, requesterId, settlementStarted));
    }

    /**
     * 약속 날짜 변경 (방장만 가능, null로 보내면 날짜 미정으로 되돌림)
     */
    @PatchMapping("/{code}/meeting-date")
    public ResponseEntity<RoomDetailResponse> updateMeetingDate(@PathVariable String code,
            @AuthenticationPrincipal UUID requesterId, @RequestBody RoomMeetingDateUpdateRequest request) {
        Room room = roomService.updateMeetingDate(code, requesterId, request.getMeetingDate());
        List<Participant> participants = participantService.getParticipants(code);
        boolean settlementStarted = settlementService.existsForRoom(room);
        return ResponseEntity.ok(RoomDetailResponse.from(room, participants, requesterId, settlementStarted));
    }

    /**
     * 방 삭제 (방장만 가능)
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String code, @AuthenticationPrincipal UUID requesterId) {
        roomService.deleteRoom(code, requesterId);
        return ResponseEntity.noContent().build();
    }
}
