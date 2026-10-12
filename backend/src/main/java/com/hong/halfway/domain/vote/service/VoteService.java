package com.hong.halfway.domain.vote.service;

import com.hong.halfway.domain.midpoint.dto.MidpointCandidateResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointResultResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointRoundTripCandidateResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointRoundTripTimeResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointStatus;
import com.hong.halfway.domain.midpoint.dto.MidpointWithHomeResultResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointWithHomeStatus;
import com.hong.halfway.domain.midpoint.service.MidpointService;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.service.StationService;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.vote.dto.ParticipantTimeResponse;
import com.hong.halfway.domain.vote.dto.StationCandidateResponse;
import com.hong.halfway.domain.vote.dto.VoteResultResponse;
import com.hong.halfway.domain.vote.entity.StationCandidate;
import com.hong.halfway.domain.vote.entity.StationCandidateTime;
import com.hong.halfway.domain.vote.entity.StationVote;
import com.hong.halfway.domain.vote.repository.StationCandidateRepository;
import com.hong.halfway.domain.vote.repository.StationCandidateTimeRepository;
import com.hong.halfway.domain.vote.repository.StationVoteRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final StationCandidateRepository stationCandidateRepository;
    private final StationCandidateTimeRepository stationCandidateTimeRepository;
    private final StationVoteRepository stationVoteRepository;
    private final MidpointService midpointService;
    private final StationService stationService;
    private final StationRepository stationRepository;

    /**
     * 투표 시작. 현재 중간지점 후보를 그대로 스냅샷 저장하고 방 상태를 VOTING으로 전환 (방장만 가능)
     * considerHome이면 귀가역까지 고려한 후보(출발+귀가 합산 시간 기준)로 스냅샷
     */
    @Transactional
    public void startVoting(String code, boolean considerHome, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.COLLECTING) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }

        if (considerHome) {
            snapshotFromHomeMidpoint(room, code);
        } else {
            snapshotFromDepartureMidpoint(room, code);
        }

        room.startVoting();
        log.info("[완료] 투표 시작 - code: {}, 귀가역 고려: {}", code, considerHome);
    }

    // 출발역 기준 중간지점 후보를 StationCandidate로 스냅샷
    private void snapshotFromDepartureMidpoint(Room room, String code) {
        MidpointResultResponse midpoint = midpointService.getMidpointResult(code);
        if (midpoint.getStatus() != MidpointStatus.READY || midpoint.getCandidates().isEmpty()) {
            throw new CustomException(ErrorCode.CANDIDATES_NOT_READY);
        }

        int rank = 1;
        for (MidpointCandidateResponse candidate : midpoint.getCandidates()) {
            StationCandidate saved = stationCandidateRepository.save(StationCandidate.snapshot(room, candidate.getStationName(),
                    rank++, candidate.getFairnessScore(), candidate.getMaxMinutes(), candidate.getSumMinutes(), false));
            for (com.hong.halfway.domain.midpoint.dto.ParticipantTimeResponse time : candidate.getTimes()) {
                Participant participant = participantRepository.getReferenceById(time.getParticipantId());
                stationCandidateTimeRepository.save(StationCandidateTime.snapshot(saved, participant, time.getMinutes()));
            }
        }
    }

    // 귀가역까지 고려한 후보를 StationCandidate로 스냅샷. 출발·귀가 구간을 따로 저장해서
    // 투표 화면에서도 선택 화면과 동일하게 구간별로 보여줄 수 있게 함
    private void snapshotFromHomeMidpoint(Room room, String code) {
        MidpointWithHomeResultResponse midpoint = midpointService.getMidpointResultWithHome(code);
        if (midpoint.getStatus() != MidpointWithHomeStatus.READY || midpoint.getCandidates().isEmpty()) {
            throw new CustomException(ErrorCode.CANDIDATES_NOT_READY);
        }

        int rank = 1;
        for (MidpointRoundTripCandidateResponse candidate : midpoint.getCandidates()) {
            List<Integer> combinedMinutesList = candidate.getTimes().stream()
                    .map(time -> time.getDepartureMinutes() + time.getHomeMinutes())
                    .toList();
            int maxMinutes = combinedMinutesList.stream().mapToInt(Integer::intValue).max().orElse(0);
            int sumMinutes = combinedMinutesList.stream().mapToInt(Integer::intValue).sum();

            StationCandidate saved = stationCandidateRepository.save(StationCandidate.snapshot(room, candidate.getStationName(),
                    rank++, candidate.getFairnessScore(), maxMinutes, sumMinutes, true));
            for (MidpointRoundTripTimeResponse time : candidate.getTimes()) {
                Participant participant = participantRepository.getReferenceById(time.getParticipantId());
                stationCandidateTimeRepository.save(StationCandidateTime.snapshotWithHome(saved, participant,
                        time.getDepartureMinutes(), time.getHomeMinutes()));
            }
        }
    }

    /**
     * 투표 취소. 고정된 후보·체크 기록을 모두 지우고 출발지 수집 상태로 복귀 (방장만 가능)
     */
    @Transactional
    public void cancelVoting(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.VOTING) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }

        deleteAllCandidates(room);

        room.cancelVoting();
        log.info("[완료] 투표 취소 - code: {}", code);
    }

    /**
     * 이 방의 후보·체크·소요시간 스냅샷을 모두 삭제 (방 삭제 시 room 도메인에서 호출)
     */
    @Transactional
    public void deleteAllCandidates(Room room) {
        List<StationCandidate> candidates = stationCandidateRepository.findAllByRoomOrderByRankAsc(room);
        stationVoteRepository.deleteAll(stationVoteRepository.findAllByCandidateIn(candidates));
        stationCandidateTimeRepository.deleteAll(stationCandidateTimeRepository.findAllByCandidateIn(candidates));
        stationCandidateRepository.deleteAll(candidates);
    }

    /**
     * 후보 목록 + 참여자별 소요시간 + 득표 수 + 1등 후보 조회
     */
    public VoteResultResponse getCandidates(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        List<StationCandidate> candidates = stationCandidateRepository.findAllByRoomOrderByRankAsc(room);

        Map<Long, List<StationCandidateTime>> timesByCandidateId = stationCandidateTimeRepository.findAllByCandidateIn(candidates)
                .stream()
                .collect(Collectors.groupingBy(time -> time.getCandidate().getId()));
        Map<Long, List<StationVote>> votesByCandidateId = stationVoteRepository.findAllByCandidateIn(candidates).stream()
                .collect(Collectors.groupingBy(vote -> vote.getCandidate().getId()));

        List<StationCandidateResponse> responses = candidates.stream()
                .map(candidate -> toResponse(candidate, timesByCandidateId, votesByCandidateId, requesterId))
                .toList();

        return VoteResultResponse.builder()
                .candidates(responses)
                .winnerCandidateId(determineWinner(responses))
                .build();
    }

    /**
     * 후보 체크 ("괜찮아요"). 이미 체크했으면 그대로 둠
     */
    @Transactional
    public void vote(String code, Long candidateId, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        StationCandidate candidate = findCandidateInRoom(candidateId, participant.getRoom());
        if (participant.getRoom().getStatus() != RoomStatus.VOTING) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }

        if (stationVoteRepository.findByCandidateAndParticipant(candidate, participant).isEmpty()) {
            stationVoteRepository.save(StationVote.check(candidate, participant));
        }
        log.info("[완료] 후보 체크 - code: {}, candidateId: {}", code, candidateId);
    }

    /**
     * 후보 체크 해제
     */
    @Transactional
    public void unvote(String code, Long candidateId, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        StationCandidate candidate = findCandidateInRoom(candidateId, participant.getRoom());

        stationVoteRepository.findByCandidateAndParticipant(candidate, participant)
                .ifPresent(stationVoteRepository::delete);
        log.info("[완료] 후보 체크 해제 - code: {}, candidateId: {}", code, candidateId);
    }

    /**
     * 후보 중 하나를 약속 장소로 확정. 득표 1등이 아니어도 방장이 원하는 후보를 바로 확정할 수 있음 (방장만 가능)
     */
    @Transactional
    public void confirmCandidate(String code, Long candidateId, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.VOTING) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }

        StationCandidate candidate = findCandidateInRoom(candidateId, room);
        room.confirm(candidate.getStationName());
        log.info("[완료] 장소 확정 - code: {}, station: {}", code, candidate.getStationName());
    }

    /**
     * 확정 취소 후 투표 상태로 복귀. 후보·체크 기록은 그대로 유지 (방장만 가능)
     */
    @Transactional
    public void unconfirm(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }
        if (room.getMode() == RoomMode.FIXED) {
            throw new CustomException(ErrorCode.FIXED_ROOM_CANNOT_UNCONFIRM);
        }
        if (room.getConfirmedFoodPlaceName() != null || room.getConfirmedCafePlaceName() != null) {
            throw new CustomException(ErrorCode.PLACE_STILL_CONFIRMED);
        }

        room.unconfirm();
        log.info("[완료] 확정 취소 - code: {}", code);
    }

    /**
     * FIXED 모드 방의 확정 역 수정. 음식점·카페를 아직 하나도 안 골랐을 때만 가능 (방장만 가능)
     */
    @Transactional
    public void updateFixedStation(String code, String stationName, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getMode() != RoomMode.FIXED || room.getStatus() != RoomStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }
        if (room.getConfirmedFoodPlaceName() != null || room.getConfirmedCafePlaceName() != null) {
            throw new CustomException(ErrorCode.PLACE_STILL_CONFIRMED);
        }
        if (stationRepository.findAllByName(stationName).isEmpty()) {
            throw new CustomException(ErrorCode.STATION_NOT_FOUND);
        }

        room.changeFixedStation(stationName);
        log.info("[완료] 고정 역 수정 - code: {}, station: {}", code, stationName);

        try {
            stationService.ensureCoordinates(stationName);
        } catch (Exception e) {
            log.warn("[경고] 확정 역 좌표 조회 중 오류 - station: {}", stationName, e);
        }
    }

    // 후보 + 득표·소요시간 맵을 응답 DTO로 변환
    private StationCandidateResponse toResponse(StationCandidate candidate, Map<Long, List<StationCandidateTime>> timesByCandidateId,
            Map<Long, List<StationVote>> votesByCandidateId, UUID requesterId) {
        List<ParticipantTimeResponse> times = timesByCandidateId.getOrDefault(candidate.getId(), List.of()).stream()
                .map(time -> ParticipantTimeResponse.builder()
                        .participantId(time.getParticipant().getId())
                        .nickname(time.getParticipant().getNickname())
                        .minutes(time.getMinutes())
                        .departureMinutes(time.getDepartureMinutes() == null ? null : (int) time.getDepartureMinutes())
                        .homeMinutes(time.getHomeMinutes() == null ? null : (int) time.getHomeMinutes())
                        .build())
                .toList();
        List<StationVote> votes = votesByCandidateId.getOrDefault(candidate.getId(), List.of());
        boolean votedByMe = votes.stream()
                .anyMatch(vote -> vote.getParticipant().getUser().getId().equals(requesterId));

        return StationCandidateResponse.builder()
                .id(candidate.getId())
                .stationName(candidate.getStationName())
                .rank(candidate.getRank())
                .fairnessScore(candidate.getFairnessScore())
                .maxMinutes(candidate.getMaxMinutes())
                .sumMinutes(candidate.getSumMinutes())
                .considerHome(candidate.isConsiderHome())
                .times(times)
                .voteCount(votes.size())
                .votedByMe(votedByMe)
                .build();
    }

    // 득표 수 내림차순 → 공평도 내림차순으로 1등 결정, 동점이면 null (docs/DOMAIN.md 3절)
    private Long determineWinner(List<StationCandidateResponse> responses) {
        List<StationCandidateResponse> sorted = responses.stream()
                .sorted(Comparator.comparingInt(StationCandidateResponse::getVoteCount)
                        .thenComparingInt(StationCandidateResponse::getFairnessScore)
                        .reversed())
                .toList();

        if (sorted.isEmpty() || sorted.get(0).getVoteCount() == 0) {
            return null;
        }
        if (sorted.size() > 1
                && sorted.get(0).getVoteCount() == sorted.get(1).getVoteCount()
                && sorted.get(0).getFairnessScore() == sorted.get(1).getFairnessScore()) {
            return null;
        }
        return sorted.get(0).getId();
    }

    // 요청자가 방장인지 확인, 아니면 예외
    private void requireOwner(Room room, UUID requesterId) {
        if (!room.getOwner().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_OWNER);
        }
    }

    // 후보가 이 방 소속인지 확인 후 조회, 아니면 예외
    private StationCandidate findCandidateInRoom(Long candidateId, Room room) {
        StationCandidate candidate = stationCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANDIDATE_NOT_FOUND));
        if (!candidate.getRoom().getId().equals(room.getId())) {
            throw new CustomException(ErrorCode.CANDIDATE_NOT_FOUND);
        }
        return candidate;
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
