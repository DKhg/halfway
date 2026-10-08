package com.hong.halfway.domain.midpoint.service;

import com.hong.halfway.domain.midpoint.dto.MidpointResultResponse;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.domain.station.repository.TravelTimeByNameProjection;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidpointService {

    // 최소 계산 가능 참여자 수 (출발역이 입력된 참여자 기준)
    private static final int MIN_PARTICIPANTS_TO_CALCULATE = 2;

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final StationRepository stationRepository;
    private final StationTravelTimeRepository stationTravelTimeRepository;

    /**
     * 참여자 출발역 기준으로 중간지점 후보 상위 3개를 즉석 계산 (결과 저장 안 함, 호출마다 재계산)
     */
    public MidpointResultResponse getMidpointResult(String code) {
        Room room = findRoomByCode(code);
        List<Participant> activeParticipants = participantRepository.findAllByRoom(room).stream()
                .filter(participant -> participant.getStartStationName() != null)
                .toList();

        if (activeParticipants.size() < MIN_PARTICIPANTS_TO_CALCULATE) {
            return MidpointResultResponse.waiting(activeParticipants.size());
        }

        Map<Long, List<Station>> stationsByParticipantId = resolveStations(activeParticipants);
        List<Long> allFromStationIds = stationsByParticipantId.values().stream()
                .flatMap(List::stream)
                .map(Station::getId)
                .toList();

        Map<Long, List<TravelTimeByNameProjection>> edgesByFromStationId = stationTravelTimeRepository
                .findMinutesFromStations(allFromStationIds).stream()
                .collect(Collectors.groupingBy(TravelTimeByNameProjection::getFromStationId));

        Map<Long, Map<String, Integer>> minutesByParticipantAndName = new LinkedHashMap<>();
        for (Participant participant : activeParticipants) {
            Map<String, Integer> minutesByName = new HashMap<>();
            for (Station myStation : stationsByParticipantId.get(participant.getId())) {
                for (TravelTimeByNameProjection edge : edgesByFromStationId.getOrDefault(myStation.getId(), List.of())) {
                    minutesByName.merge(edge.getToStationName(), (int) edge.getMinutes(), Math::min);
                }
            }
            // 본인이 이미 있는 역까지 이동시간은 0분 (같은 이름 노드 간 거리는 테이블에 없음)
            minutesByName.put(participant.getStartStationName(), 0);
            minutesByParticipantAndName.put(participant.getId(), minutesByName);
        }

        Set<String> candidateNames = intersectCandidateNames(minutesByParticipantAndName);
        Map<String, Map<Long, Integer>> minutesByCandidateAndParticipant = new HashMap<>();
        for (String candidateName : candidateNames) {
            Map<Long, Integer> perParticipant = new LinkedHashMap<>();
            for (Participant participant : activeParticipants) {
                perParticipant.put(participant.getId(), minutesByParticipantAndName.get(participant.getId()).get(candidateName));
            }
            minutesByCandidateAndParticipant.put(candidateName, perParticipant);
        }

        List<MidpointCandidate> ranked = MidpointFairnessCalculator.rank(minutesByCandidateAndParticipant, room.getFairnessType());
        Map<Long, String> nicknameByParticipantId = activeParticipants.stream()
                .collect(Collectors.toMap(Participant::getId, Participant::getNickname));

        return MidpointResultResponse.ready(ranked, nicknameByParticipantId);
    }

    // 참여자 출발역 이름을 실제 station 노드(들)로 변환, 등록 안 된 이름이면 예외
    private Map<Long, List<Station>> resolveStations(List<Participant> activeParticipants) {
        Map<Long, List<Station>> stationsByParticipantId = new LinkedHashMap<>();
        for (Participant participant : activeParticipants) {
            List<Station> stations = stationRepository.findAllByName(participant.getStartStationName());
            if (stations.isEmpty()) {
                throw new CustomException(ErrorCode.STATION_NOT_FOUND);
            }
            stationsByParticipantId.put(participant.getId(), stations);
        }
        return stationsByParticipantId;
    }

    // 모든 참여자가 도달 가능한 역 이름만 후보로 남김
    private Set<String> intersectCandidateNames(Map<Long, Map<String, Integer>> minutesByParticipantAndName) {
        Set<String> candidateNames = null;
        for (Map<String, Integer> minutesByName : minutesByParticipantAndName.values()) {
            if (candidateNames == null) {
                candidateNames = new HashSet<>(minutesByName.keySet());
            } else {
                candidateNames.retainAll(minutesByName.keySet());
            }
        }
        return candidateNames == null ? Set.of() : candidateNames;
    }

    // 코드로 방 조회, 없으면 예외
    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }
}
