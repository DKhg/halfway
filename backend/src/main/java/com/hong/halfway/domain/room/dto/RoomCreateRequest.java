package com.hong.halfway.domain.room.dto;

import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.RoomMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomCreateRequest {

    @NotBlank
    @Size(max = 30)
    private String name;

    @NotNull
    private RoomMode mode;

    // 안 정하면 서비스 레이어에서 기본값(MAX_MIN)으로 채움
    private FairnessType fairnessType;

    // 확정된 약속 날짜. 안 정하면 null(날짜 미정)로 생성됨
    private LocalDate meetingDate;

    // FIXED 모드일 때 방장이 직접 정한 역 이름. 그 외 모드에서는 무시됨
    private String fixedStationName;

    // 이 방에서 음식점 선택이 필요한지. 안 정하면 서비스 레이어에서 true로 채움
    private Boolean includesFood;

    // 이 방에서 카페 선택이 필요한지. 안 정하면 서비스 레이어에서 true로 채움
    private Boolean includesCafe;
}
