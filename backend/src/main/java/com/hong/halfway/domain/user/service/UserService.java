package com.hong.halfway.domain.user.service;

import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.place.entity.StationPlacePool;
import com.hong.halfway.domain.place.repository.StationPlacePoolRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.user.dto.UserMeResponse;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
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
public class UserService {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final StationPlacePoolRepository stationPlacePoolRepository;
    private final RoomService roomService;

    /**
     * 기기별 익명 사용자 생성 및 인증 토큰 발급
     */
    @Transactional
    public String createAnonymousUser() {
        String rawToken = AuthTokenUtils.generate();
        String tokenHash = AuthTokenUtils.hash(rawToken);
        User user = userRepository.save(User.createAnonymous(tokenHash));
        log.info("[완료] 익명 사용자 생성 - ID: {}", user.getId());
        return rawToken;
    }

    /**
     * 내 정보 조회 (계정에 저장된 기본 닉네임·귀가역)
     */
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return UserMeResponse.builder()
                .nickname(user.getLastNickname())
                .homeStationName(user.getHomeStationName())
                .build();
    }

    /**
     * 기본 닉네임 설정·수정 (마이페이지에서 직접 수정, 방 참여 화면에 자동으로 채워주는 값)
     */
    @Transactional
    public void updateNickname(UUID userId, String nickname) {
        User user = userRepository.getReferenceById(userId);
        user.updateLastNickname(nickname.trim());
        log.info("[완료] 기본 닉네임 설정 - userId: {}", userId);
    }

    /**
     * 귀가역 설정·수정. 빈 값이면 해제, 값이 있으면 실재하는 역인지 확인
     */
    @Transactional
    public void updateHomeStation(UUID userId, String homeStationName) {
        String trimmed = homeStationName == null ? null : homeStationName.trim();
        if (trimmed != null && !trimmed.isEmpty() && stationRepository.findAllByName(trimmed).isEmpty()) {
            throw new CustomException(ErrorCode.STATION_NOT_FOUND);
        }

        User user = userRepository.getReferenceById(userId);
        user.updateHomeStation(trimmed == null || trimmed.isEmpty() ? null : trimmed);
        log.info("[완료] 귀가역 설정 - userId: {}", userId);
    }

    /**
     * 내 익명 계정 삭제. 내가 방장인 방은 전체 삭제(참여자·투표·정산 포함), 참여자로만 있던 방은 내 참여 기록만 삭제,
     * 역 가게 풀에 남은 "등록자" 표시는 비우고(가게 데이터는 유지) 마지막에 계정 자체를 삭제
     */
    @Transactional
    public void deleteMyAccount(UUID userId) {
        User user = userRepository.getReferenceById(userId);

        List<Room> ownedRooms = roomRepository.findByOwnerOrderByCreatedAtDesc(user);
        for (Room room : ownedRooms) {
            roomService.deleteRoom(room.getCode(), userId);
        }

        participantRepository.deleteAll(participantRepository.findAllByUser(user));

        stationPlacePoolRepository.findAllByAddedByUser(user)
                .forEach(StationPlacePool::clearAddedByUser);

        userRepository.delete(user);
        log.info("[완료] 계정 삭제 - userId: {}", userId);
    }
}
