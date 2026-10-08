package com.hong.halfway.domain.room.service;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.place.service.PlaceService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.dto.RoomUpdateRequest;
import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.settlement.service.SettlementService;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.service.StationService;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.vote.service.VoteService;
import com.hong.halfway.global.common.RoomCodeGenerator;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    // 코드 중복 시 재생성 최대 횟수 (8자리 랜덤이라 실제 충돌은 거의 없음)
    private static final int CODE_GENERATION_MAX_ATTEMPTS = 5;

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final VoteService voteService;
    private final PlaceService placeService;
    private final SettlementService settlementService;
    private final StationRepository stationRepository;
    private final StationService stationService;

    /**
     * 새 약속 방 생성. 방장은 인증 필터에서 확인된 사용자로 고정
     * FIXED 모드면 투표 단계 없이 바로 CONFIRMED로 생성, SETTLEMENT_ONLY 모드면 역·장소 없이 바로 CONFIRMED로 생성
     */
    @Transactional
    public Room createRoom(UUID ownerId, RoomCreateRequest request) {
        // 인증 필터가 이미 토큰으로 존재를 확인했으므로 추가 조회 없이 프록시만 참조
        User owner = userRepository.getReferenceById(ownerId);
        String code = generateUniqueCode();
        LocalDate meetingDate = request.getMeetingDate();

        if (request.getMode() == RoomMode.SETTLEMENT_ONLY) {
            Room room = Room.createSettlementOnly(code, owner, request.getName(), meetingDate);
            roomRepository.save(room);
            log.info("[완료] 방 생성(정산만 하기) - code: {}", code);
            return room;
        }

        FairnessType fairnessType = request.getFairnessType() != null ? request.getFairnessType() : FairnessType.MAX_MIN;
        boolean includesFood = request.getIncludesFood() == null || request.getIncludesFood();
        boolean includesCafe = request.getIncludesCafe() == null || request.getIncludesCafe();
        if (!includesFood && !includesCafe) {
            throw new CustomException(ErrorCode.AT_LEAST_ONE_CATEGORY_REQUIRED);
        }

        Room room = request.getMode() == RoomMode.FIXED
                ? createFixedRoom(code, owner, request, fairnessType, meetingDate, includesFood, includesCafe)
                : Room.create(code, owner, request.getName(), request.getMode(), fairnessType,
                        meetingDate, includesFood, includesCafe);
        roomRepository.save(room);
        log.info("[완료] 방 생성 - code: {}", code);
        return room;
    }

    // FIXED 모드 방 생성. 입력받은 역이 실재하는지 검증 후 바로 CONFIRMED로 만들고 좌표 조회까지 트리거
    private Room createFixedRoom(String code, User owner, RoomCreateRequest request, FairnessType fairnessType,
            LocalDate meetingDate, boolean includesFood, boolean includesCafe) {
        String stationName = request.getFixedStationName();
        if (stationName == null || stationName.isBlank()) {
            throw new CustomException(ErrorCode.FIXED_STATION_REQUIRED);
        }
        if (stationRepository.findAllByName(stationName).isEmpty()) {
            throw new CustomException(ErrorCode.STATION_NOT_FOUND);
        }

        Room room = Room.createFixed(code, owner, request.getName(), fairnessType,
                meetingDate, includesFood, includesCafe, stationName);

        // 좌표 조회 실패가 방 생성 자체를 막으면 안 됨
        try {
            stationService.ensureCoordinates(stationName);
        } catch (Exception e) {
            log.warn("[경고] 확정 역 좌표 조회 중 오류 - station: {}", stationName, e);
        }
        return room;
    }

    /**
     * 코드로 방 조회 (방 상세 화면용)
     */
    public Room getRoom(String code) {
        return findRoomByCode(code);
    }

    /**
     * 내가 방장이거나 참여한 방 목록, 최신순 (내 약속 목록용)
     */
    public List<Room> getMyRooms(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        List<Room> ownedRooms = roomRepository.findByOwnerOrderByCreatedAtDesc(user);
        List<Room> joinedRooms = participantRepository.findAllByUser(user).stream()
                .map(Participant::getRoom)
                .toList();

        // 방장이면서 참여자이기도 한 경우 중복 제거
        Map<Long, Room> merged = new LinkedHashMap<>();
        ownedRooms.forEach(room -> merged.put(room.getId(), room));
        joinedRooms.forEach(room -> merged.putIfAbsent(room.getId(), room));

        return merged.values().stream()
                .sorted(Comparator.comparing(Room::getCreatedAt).reversed())
                .toList();
    }

    /**
     * 방 설정 변경 (공평 기준). 방장만 가능
     */
    @Transactional
    public Room updateRoom(String code, UUID requesterId, RoomUpdateRequest request) {
        Room room = findRoomByCode(code);
        if (!room.getOwner().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_OWNER);
        }

        room.changeFairnessType(request.getFairnessType());
        log.info("[완료] 방 설정 변경 - code: {}", code);
        return room;
    }

    /**
     * 약속 날짜 변경. 참여자면 누구나 가능(방장 제한 없음), 정산이 시작된 뒤에는 변경 불가. null로 보내면 다시 날짜 미정으로 되돌림
     */
    @Transactional
    public Room updateMeetingDate(String code, UUID requesterId, LocalDate meetingDate) {
        Room room = findRoomByCode(code);
        requireParticipantOrOwner(room, requesterId);
        if (settlementService.existsForRoom(room)) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }

        room.changeMeetingDate(meetingDate);
        log.info("[완료] 약속 날짜 변경 - code: {}", code);
        return room;
    }

    // 방장이거나 이 방에 참여한 사람인지 확인, 아니면 예외
    private void requireParticipantOrOwner(Room room, UUID requesterId) {
        if (room.getOwner().getId().equals(requesterId)) {
            return;
        }
        User requester = userRepository.getReferenceById(requesterId);
        if (!participantRepository.existsByRoomAndUser(room, requester)) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
    }

    /**
     * 방 삭제. 방장만 가능, 참여자 기록도 함께 삭제
     */
    @Transactional
    public void deleteRoom(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        if (!room.getOwner().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_OWNER);
        }

        // FK에 CASCADE가 없어서 참조 순서 역방향으로 먼저 지워야 함: 투표·정산 기록 → 참여자 → 방
        voteService.deleteAllCandidates(room);
        placeService.deleteAllVotes(room);
        settlementService.deleteAllForRoom(room);
        participantRepository.deleteAll(participantRepository.findAllByRoom(room));
        roomRepository.delete(room);
        log.info("[완료] 방 삭제 - code: {}", code);
    }

    // 코드로 방 조회, 없으면 예외
    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    // 중복 안 되는 방 코드 생성, 충돌하면 재시도
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_ATTEMPTS; attempt++) {
            String code = RoomCodeGenerator.generate();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        log.error("[에러] 방 코드 생성 {}회 연속 충돌", CODE_GENERATION_MAX_ATTEMPTS);
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
