package com.hong.halfway.domain.room.dto;

import com.hong.halfway.domain.participant.dto.ParticipantSummaryResponse;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RoomDetailResponse {

    private final String code;

    private final String name;

    private final RoomMode mode;

    private final FairnessType fairnessType;

    private final LocalDate meetingDate;

    private final RoomStatus status;

    // 확정된 약속 장소 역 이름. 확정 전엔 null
    private final String confirmedStationName;

    // 확정된 약속 음식점 이름. 역만 확정되고 음식점은 아직 안 골랐으면 null
    private final String confirmedFoodPlaceName;

    // 확정된 약속 카페 이름. 역만 확정되고 카페는 아직 안 골랐으면 null
    private final String confirmedCafePlaceName;

    // 이 방에서 음식점 선택이 필요한지 여부
    private final boolean includesFood;

    // 이 방에서 카페 선택이 필요한지 여부
    private final boolean includesCafe;

    // 요청자가 방장인지 여부. 방장 전용 액션(장소·일시 확정 등) 노출 판단에 사용
    private final boolean isOwner;

    // 정산이 이미 시작됐는지(결제자가 선점됐는지) 여부. 시작 전엔 정산 탭으로 이동할 수 없게 하는 데 사용
    private final boolean settlementStarted;

    private final List<ParticipantSummaryResponse> participants;

    // Room 엔티티와 참여자 목록을 응답 DTO로 변환
    public static RoomDetailResponse from(Room room, List<Participant> participants, UUID requesterId,
            boolean settlementStarted) {
        return RoomDetailResponse.builder()
                .code(room.getCode())
                .name(room.getName())
                .mode(room.getMode())
                .fairnessType(room.getFairnessType())
                .meetingDate(room.getMeetingDate())
                .status(room.getStatus())
                .confirmedStationName(room.getConfirmedStationName())
                .confirmedFoodPlaceName(room.getConfirmedFoodPlaceName())
                .confirmedCafePlaceName(room.getConfirmedCafePlaceName())
                .includesFood(room.isIncludesFood())
                .includesCafe(room.isIncludesCafe())
                .isOwner(room.getOwner().getId().equals(requesterId))
                .settlementStarted(settlementStarted)
                .participants(participants.stream()
                        .map(participant -> ParticipantSummaryResponse.from(participant, requesterId))
                        .toList())
                .build();
    }
}
