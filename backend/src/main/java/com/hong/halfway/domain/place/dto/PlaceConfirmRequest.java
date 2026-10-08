package com.hong.halfway.domain.place.dto;

import com.hong.halfway.domain.place.entity.PlaceCategory;
import jakarta.validation.constraints.NotBlank;
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
public class PlaceConfirmRequest {

    @NotNull
    private PlaceCategory category;

    @NotBlank
    private String placeName;
}
