package com.hong.halfway.domain.place.entity;

import java.util.UUID;

/**
 * 주변 장소 후보 하나 (카카오 로컬 API 검색 결과 또는 참여자가 직접 추가한 곳). id는 카카오 장소 id로, 같은 상호가 여러 지점일 때 구분용
 */
public record PlaceOption(String id, String name, PlaceCategory category, FoodSubCategory subCategory,
        String roadAddressName, String placeUrl, Integer walkMinutes, boolean custom, String comment,
        UUID addedByUserId) {
}
