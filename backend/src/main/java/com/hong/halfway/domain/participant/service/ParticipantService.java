package com.hong.halfway.domain.participant.service;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * 방 참여. 이미 참여한 방이면 거절, 성공하면 다음 참여를 위해 닉네임 기억
     */
    @Transactional
    public Participant join(String code, UUID userId, String nickname) {
        Room room = findRoomByCode(code);
        // FIXED·SETTLEMENT_ONLY 모드는 CONFIRMED가 생성 시점부터 이어지는 정상 상태라 역이 노출돼도 상관없어서 예외. CLOSED는 모드와 무관하게 항상 막음
        boolean skipsVotingFlow = room.getMode() == RoomMode.FIXED || room.getMode() == RoomMode.SETTLEMENT_ONLY;
        boolean placeHiddenFromLateJoiners = !skipsVotingFlow && room.getStatus() == RoomStatus.CONFIRMED;
        if (placeHiddenFromLateJoiners || room.getStatus() == RoomStatus.CLOSED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_CONFIRMED);
        }
        User user = userRepository.getReferenceById(userId);

        if (participantRepository.existsByRoomAndUser(room, user)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }

        Participant participant = participantRepository.save(Participant.join(room, user, nickname));
        user.updateLastNickname(nickname);
        log.info("[완료] 방 참여 - code: {}", code);
        return participant;
    }

    /**
     * 출발역·귀가역 입력 또는 수정. 귀가역을 안 정하면 출발역과 동일하게 처리
     */
    @Transactional
    public Participant updateMyInfo(String code, UUID userId, ParticipantUpdateRequest request) {
        Participant participant = findMyParticipant(code, userId);
        String homeStationName = request.getHomeStationName() != null && !request.getHomeStationName().isBlank()
                ? request.getHomeStationName()
                : request.getStartStationName();

        participant.updateStations(request.getStartStationName(), homeStationName);
        log.info("[완료] 참여 정보 수정 - code: {}", code);
        return participant;
    }

    /**
     * 방 참여자 전체 목록 조회 (방 상세 화면용)
     */
    public List<Participant> getParticipants(String code) {
        Room room = findRoomByCode(code);
        return participantRepository.findAllByRoom(room);
    }

    /**
     * 방 참여자 수 조회 (내 약속 목록 카드용)
     */
    public long countParticipants(Room room) {
        return participantRepository.countByRoom(room);
    }

    /**
     * 내 참여 정보 삭제
     */
    @Transactional
    public void leave(String code, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        participantRepository.delete(participant);
        log.info("[완료] 참여 취소 - code: {}", code);
    }

    // 요청자 본인의 참여 정보 조회, 없으면 예외
    private Participant findMyParticipant(String code, UUID userId) {
        Room room = findRoomByCode(code);
        User user = userRepository.getReferenceById(userId);
        return participantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    // 코드로 방 조회, 없으면 예외
    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }
}
