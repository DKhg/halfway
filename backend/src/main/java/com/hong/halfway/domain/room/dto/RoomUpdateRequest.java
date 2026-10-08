package com.hong.halfway.domain.room.dto;

import com.hong.halfway.domain.room.entity.FairnessType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomUpdateRequest {

    @NotNull
    private FairnessType fairnessType;
}
