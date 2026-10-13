package com.hong.halfway.domain.place.repository;

import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.StationPlacePool;
import com.hong.halfway.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationPlacePoolRepository extends JpaRepository<StationPlacePool, Long> {

    // 같은 역에 쌓인 가게 풀 조회 ("다른 모임이 추가한 곳" 제안용)
    List<StationPlacePool> findAllByStationNameAndCategory(String stationName, PlaceCategory category);

    // 중복 적재 방지용. 동시 요청에 의한 경합은 DB 유니크 제약이 최종 방어선
    boolean existsByStationNameAndCategoryAndPlaceId(String stationName, PlaceCategory category, String placeId);

    // 계정 삭제 시 이 사용자가 등록자로 남아있는 행을 찾아 표시만 지우기 위함
    List<StationPlacePool> findAllByAddedByUser(User addedByUser);
}
