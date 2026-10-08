package com.hong.halfway.domain.place.repository;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.PlaceVote;
import com.hong.halfway.domain.room.entity.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceVoteRepository extends JpaRepository<PlaceVote, Long> {

    // 가게 목록 조회 시 카테고리별 득표를 한 번에 불러올 때 사용
    List<PlaceVote> findAllByRoomAndCategory(Room room, PlaceCategory category);

    // 체크·체크 해제 시 중복 여부 확인
    Optional<PlaceVote> findByRoomAndParticipantAndCategoryAndPlaceId(Room room, Participant participant,
            PlaceCategory category, String placeId);

    // 방 삭제 시 이 방의 가게 투표 기록을 모두 정리
    void deleteAllByRoom(Room room);

    // 직접 추가한 가게 삭제 시, 그 가게에 달린 체크 기록도 함께 정리
    void deleteAllByRoomAndCategoryAndPlaceId(Room room, PlaceCategory category, String placeId);
}
