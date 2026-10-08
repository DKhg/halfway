package com.hong.halfway.domain.place.dto;

import com.hong.halfway.domain.place.entity.FoodSubCategory;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.PlaceOption;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PlaceOptionResponse {

    private final String id;

    private final String name;

    private final PlaceCategory category;

    private final FoodSubCategory subCategory;

    // 도로명 주소. 같은 상호 여러 지점을 구분하는 용도
    private final String roadAddressName;

    // 카카오맵 장소 상세 페이지 링크 (사진·리뷰 확인용)
    private final String placeUrl;

    // 확정 역에서 도보 소요시간(분). 거리 정보가 없으면 null
    private final Integer walkMinutes;

    // 이 가게에 "좋아요" 체크한 참여자 수
    private final int voteCount;

    // 요청자 본인이 체크했는지 여부
    private final boolean votedByMe;

    // 추천 5개가 아니라 참여자가 직접 검색해서 추가한 곳인지 여부
    private final boolean custom;

    // 추가한 사람이 남긴 짧은 한줄평 (선택, 최대 20자). 없으면 null
    private final String comment;

    // 요청자 본인이 직접 추가한 곳인지 여부. 본인 또는 방장이면 삭제 가능
    private final boolean addedByMe;

    public static PlaceOptionResponse from(PlaceOption place, int voteCount, boolean votedByMe, UUID requesterId) {
        return PlaceOptionResponse.builder()
                .id(place.id())
                .name(place.name())
                .category(place.category())
                .subCategory(place.subCategory())
                .roadAddressName(place.roadAddressName())
                .placeUrl(place.placeUrl())
                .walkMinutes(place.walkMinutes())
                .voteCount(voteCount)
                .votedByMe(votedByMe)
                .custom(place.custom())
                .comment(place.comment())
                .addedByMe(place.addedByUserId() != null && place.addedByUserId().equals(requesterId))
                .build();
    }
}
