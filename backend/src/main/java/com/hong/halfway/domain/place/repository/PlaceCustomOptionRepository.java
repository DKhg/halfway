package com.hong.halfway.domain.place.repository;

import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.PlaceCustomOption;
import com.hong.halfway.domain.room.entity.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceCustomOptionRepository extends JpaRepository<PlaceCustomOption, Long> {

    // 가게 목록 조회 시 카테고리별로 직접 추가된 후보를 함께 불러올 때 사용
    List<PlaceCustomOption> findAllByRoomAndCategory(Room room, PlaceCategory category);

    // 중복 추가 방지용. 동시 요청에 의한 경합은 DB 유니크 제약이 최종 방어선
    boolean existsByRoomAndCategoryAndPlaceId(Room room, PlaceCategory category, String placeId);

    // 직접 추가한 가게 삭제 시 대상 조회, 삭제 권한(본인인지) 확인에 사용
    Optional<PlaceCustomOption> findByRoomAndCategoryAndPlaceId(Room room, PlaceCategory category, String placeId);

    // 방 삭제 시 이 방에서 직접 추가된 가게 후보를 모두 정리
    void deleteAllByRoom(Room room);
}
