package com.hong.halfway.domain.place.dto;

import com.hong.halfway.domain.place.entity.PlaceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlaceCustomAddRequest {

    @NotNull
    private PlaceCategory category;

    // 검색 결과에서 선택한 카카오 장소 id, 이름, 주소 등을 그대로 추가 시점 스냅샷으로 저장
    @NotBlank
    private String placeId;

    @NotBlank
    private String placeName;

    private String roadAddressName;

    private String placeUrl;

    private Integer walkMinutes;

    // 짧은 한줄평 (선택, 최대 20자)
    @Size(max = 20)
    private String comment;
}
