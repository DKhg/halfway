package com.hong.halfway.domain.lasttrain.service;

import com.hong.halfway.domain.lasttrain.dto.LastTrainDayType;
import com.hong.halfway.domain.lasttrain.dto.LastTrainResponse;
import com.hong.halfway.domain.lasttrain.repository.LatestTrainHopProjection;
import com.hong.halfway.domain.lasttrain.repository.TrainTimetableRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.entity.StationTravelTime;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 막차 역산 계산. 약속역(S)과 참여자가 입력한 귀가역(H) 사이 최단경로(predecessor 체인)를 복원해
 * 구간마다 실제 열차 시간표로 "S에서 몇 시까지 출발해야 귀가역까지 갈 수 있는지" 뒤에서부터 계산
 * (대체 환승경로 비교 없이 최단경로 1개만 역산하는 1차 버전)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LastTrainService {

    // 시간표 조회 시 "아직 데드라인 없음"을 표현하는 충분히 큰 값 (실제 분 값은 1500대를 넘지 않음)
    private static final int NO_DEADLINE = 10_000;

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final RoomRepository roomRepository;
    private final StationRepository stationRepository;
    private final StationTravelTimeRepository stationTravelTimeRepository;
    private final TrainTimetableRepository trainTimetableRepository;

    /**
     * 막차 계산. 역이 아직 확정되지 않았거나 귀가역 이름이 등록되지 않았으면 예외
     */
    public LastTrainResponse calculate(String code, String homeStationName, LastTrainDayType dayType) {
        Room room = findRoomByCode(code);
        if (room.getConfirmedStationName() == null) {
            throw new CustomException(ErrorCode.ROOM_STATION_NOT_CONFIRMED);
        }

        List<Station> fromCandidates = stationRepository.findAllByName(room.getConfirmedStationName());
        List<Station> toCandidates = stationRepository.findAllByName(homeStationName);
        if (toCandidates.isEmpty()) {
            throw new CustomException(ErrorCode.STATION_NOT_FOUND);
        }

        if (hasSameStation(fromCandidates, toCandidates)) {
            return LastTrainResponse.of(null);
        }

        StationTravelTime bestPair = findBestPair(fromCandidates, toCandidates);
        if (bestPair == null) {
            return LastTrainResponse.of(null);
        }

        List<Station> path = reconstructPath(bestPair);
        if (!isFullySupported(path)) {
            return LastTrainResponse.unsupported();
        }

        Integer departureMinute = reverseCalculate(path, dayType.getWeekTag());
        return LastTrainResponse.of(departureMinute == null ? null : formatMinute(departureMinute));
    }

    // 같은 역(물리적으로 동일 노드)이면 이미 귀가역에 있는 것 - 막차 계산 자체가 필요 없음
    private boolean hasSameStation(List<Station> fromCandidates, List<Station> toCandidates) {
        return fromCandidates.stream().map(Station::getId)
                .anyMatch(id -> toCandidates.stream().anyMatch(to -> to.getId().equals(id)));
    }

    // S 후보들과 H 후보들의 모든 조합 중 최단시간 쌍 선택 (환승역처럼 같은 이름이 여러 노선에 걸쳐 있을 수 있어서)
    private StationTravelTime findBestPair(List<Station> fromCandidates, List<Station> toCandidates) {
        List<Long> fromIds = fromCandidates.stream().map(Station::getId).toList();
        List<Long> toIds = toCandidates.stream().map(Station::getId).toList();
        return stationTravelTimeRepository.findByFromStation_IdInAndToStation_IdIn(fromIds, toIds).stream()
                .min(Comparator.comparing(StationTravelTime::getMinutes))
                .orElse(null);
    }

    // predecessor 체인을 S까지 한 칸씩 따라가며 실제 경유역 순서([S, ..., H]) 복원
    private List<Station> reconstructPath(StationTravelTime bestPair) {
        Long sId = bestPair.getFromStation().getId();
        List<Long> reversedIds = new ArrayList<>();
        StationTravelTime currentRow = bestPair;
        Long currentId = currentRow.getToStation().getId();
        reversedIds.add(currentId);

        while (!currentId.equals(sId)) {
            Long predecessorId = currentRow.getPredecessorStation().getId();
            reversedIds.add(predecessorId);
            if (predecessorId.equals(sId)) {
                break;
            }
            currentRow = stationTravelTimeRepository.findByFromStation_IdAndToStation_Id(sId, predecessorId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
            currentId = predecessorId;
        }

        Collections.reverse(reversedIds);
        Map<Long, Station> stationsById = stationRepository.findAllById(reversedIds).stream()
                .collect(Collectors.toMap(Station::getId, station -> station));
        return reversedIds.stream().map(stationsById::get).toList();
    }

    // 경로의 모든 승차 구간(역 이름이 바뀌는 구간)이 시간표 데이터가 있는 역(fr_code 있음)으로만 이어져 있는지 확인
    // 환승 구간(같은 이름, 다른 노선)은 실제 승차가 아니라 도보라 시간표 데이터가 필요 없음
    private boolean isFullySupported(List<Station> path) {
        for (int i = 1; i < path.size(); i++) {
            Station from = path.get(i - 1);
            Station to = path.get(i);
            boolean isRideHop = !from.getName().equals(to.getName());
            if (isRideHop && (!isTimetableSupported(from) || !isTimetableSupported(to))) {
                return false;
            }
        }
        return true;
    }

    // frCode(서울교통공사 1~9호선) 또는 tagoStationId(그 외 노선)가 있으면 열차 시간표(train_timetable)가 적재된 역
    private boolean isTimetableSupported(Station station) {
        return station.getFrCode() != null || station.getTagoStationId() != null;
    }

    // 경로를 H에서 S 방향으로 거슬러 올라가며, 각 구간을 탈 수 있는 가장 늦은 시각을 역산
    // 승차 구간은 실제 열차(train_no)를 조회하고, 환승 구간은 이미 계산된 환승 소요시간만큼 데드라인을 당김
    private Integer reverseCalculate(List<Station> path, short weekTag) {
        int deadline = NO_DEADLINE;
        for (int i = path.size() - 1; i >= 1; i--) {
            Station from = path.get(i - 1);
            Station to = path.get(i);
            if (from.getName().equals(to.getName())) {
                StationTravelTime transferEdge = stationTravelTimeRepository
                        .findByFromStation_IdAndToStation_Id(from.getId(), to.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
                deadline -= transferEdge.getMinutes();
                continue;
            }

            List<LatestTrainHopProjection> hops = trainTimetableRepository
                    .findHopsArrivingBefore(from.getId(), to.getId(), weekTag, deadline);
            if (hops.isEmpty()) {
                return null;
            }
            deadline = hops.get(0).getLeaveMinute();
        }
        return deadline;
    }

    // 1442분(24시 넘어가는 분단위 표현) -> "00:02"
    private String formatMinute(int minuteOfDay) {
        int displayMinute = minuteOfDay % MINUTES_PER_DAY;
        return String.format("%02d:%02d", displayMinute / 60, displayMinute % 60);
    }

    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }
}
