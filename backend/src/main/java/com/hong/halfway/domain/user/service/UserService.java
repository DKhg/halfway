package com.hong.halfway.domain.user.service;

import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.user.dto.UserMeResponse;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
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
     * 내 정보 조회 (계정에 저장된 귀가역)
     */
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return UserMeResponse.builder().homeStationName(user.getHomeStationName()).build();
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
}
