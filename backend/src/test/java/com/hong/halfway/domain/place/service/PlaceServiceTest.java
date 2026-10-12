package com.hong.halfway.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.place.dto.PlaceCustomAddRequest;
import com.hong.halfway.domain.place.dto.PlaceOptionResponse;
import com.hong.halfway.domain.place.entity.FoodSubCategory;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.StationPlacePool;
import com.hong.halfway.domain.place.repository.StationPlacePoolRepository;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.station.client.KakaoLocalClient;
import com.hong.halfway.domain.station.client.dto.KakaoPlaceSearchResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.entity.StationTravelTime;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.vote.service.VoteService;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PlaceServiceTest {

    @Autowired
    private PlaceService placeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private VoteService voteService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    @Autowired
    private StationPlacePoolRepository stationPlacePoolRepository;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    // 역 확정까지 끝난 방 하나를 만들어 가게 투표·확정 테스트에 재사용 (역 좌표 조회는 성공한다고 가정)
    private Room createRoomWithStationConfirmed(String suffix, UUID[] userIdHolder) {
        when(kakaoLocalClient.searchStationCoordinates(any()))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.0, 127.0)));

        Station a = stationRepository.save(Station.create("A가게테스트역" + suffix, "테스트선"));
        Station b = stationRepository.save(Station.create("B가게테스트역" + suffix, "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));

        User owner = userRepository.save(User.createAnonymous("place-owner-" + suffix));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("가게 투표 테스트").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), owner.getId(), "방장");
        participantService.updateMyInfo(room.getCode(), owner.getId(), ParticipantUpdateRequest.builder()
                .startStationName("A가게테스트역" + suffix).build());

        User guest = userRepository.save(User.createAnonymous("place-guest-" + suffix));
        participantService.join(room.getCode(), guest.getId(), "손님");
        participantService.updateMyInfo(room.getCode(), guest.getId(), ParticipantUpdateRequest.builder()
                .startStationName("B가게테스트역" + suffix).build());

        voteService.startVoting(room.getCode(), false, owner.getId());
        Long candidateId = voteService.getCandidates(room.getCode(), owner.getId()).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, owner.getId());

        userIdHolder[0] = owner.getId();
        userIdHolder[1] = guest.getId();
        return room;
    }

    // 음식점 검색 결과를 고정 응답으로 스텁
    private void stubFoodSearch(String keyword, List<KakaoPlaceSearchResponse.Document> documents) {
        when(kakaoLocalClient.searchPlacesByKeyword(eq(keyword), eq("FD6"), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(documents);
    }

    // 카페 검색 결과를 고정 응답으로 스텁
    private void stubCafeSearch(List<KakaoPlaceSearchResponse.Document> documents) {
        when(kakaoLocalClient.searchPlacesByCategory(eq("CE7"), any(), anyInt(), anyInt())).thenReturn(documents);
    }

    private KakaoPlaceSearchResponse.Document document(String id, String name) {
        return new KakaoPlaceSearchResponse.Document(id, name, "FD6", "서울 테스트구 테스트로 1",
                "http://place.map.kakao.com/" + id, "500", "127.0", "37.0");
    }

    @Test
    void 카테고리와_세부카테고리로_검색한다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("1", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집"), document("2", "역전국밥")));

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD,
                FoodSubCategory.KOREAN, false, userIds[0]);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(place -> place.getCategory() == PlaceCategory.FOOD
                && place.getSubCategory() == FoodSubCategory.KOREAN);
    }

    @Test
    void 카페는_세부카테고리_없이_카테고리_검색을_쓴다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("2", userIds);
        stubCafeSearch(List.of(document("3", "카페온도")));

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null,
                false, userIds[0]);

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(place -> place.getCategory() == PlaceCategory.CAFE && place.getSubCategory() == null);
    }

    @Test
    void 거리로_도보시간을_계산하고_주소_링크도_함께_내려준다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("13", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));

        PlaceOptionResponse option = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD, FoodSubCategory.KOREAN,
                false, userIds[0]).get(0);

        // 도보 시간 = ceil(500 × 1.3 ÷ 80) = 9
        assertThat(option.getWalkMinutes()).isEqualTo(9);
        assertThat(option.getRoadAddressName()).isEqualTo("서울 테스트구 테스트로 1");
        assertThat(option.getPlaceUrl()).isEqualTo("http://place.map.kakao.com/1");
    }

    @Test
    void 역_좌표를_구하지_못하면_예외가_발생한다() {
        UUID[] userIds = new UUID[2];
        when(kakaoLocalClient.searchStationCoordinates(any())).thenReturn(Optional.empty());
        Station a = stationRepository.save(Station.create("A좌표실패역", "테스트선"));
        Station b = stationRepository.save(Station.create("B좌표실패역", "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));
        User owner = userRepository.save(User.createAnonymous("place-owner-coord-fail"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("좌표실패 테스트").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), owner.getId(), "방장");
        participantService.updateMyInfo(room.getCode(), owner.getId(), ParticipantUpdateRequest.builder()
                .startStationName("A좌표실패역").build());
        User guest = userRepository.save(User.createAnonymous("place-guest-coord-fail"));
        participantService.join(room.getCode(), guest.getId(), "손님");
        participantService.updateMyInfo(room.getCode(), guest.getId(), ParticipantUpdateRequest.builder()
                .startStationName("B좌표실패역").build());
        voteService.startVoting(room.getCode(), false, owner.getId());
        Long candidateId = voteService.getCandidates(room.getCode(), owner.getId()).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, owner.getId());

        assertThatThrownBy(() -> placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, false, owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATION_COORDINATES_NOT_READY);
    }

    @Test
    void 참여자가_가게를_체크하면_득표수가_올라간다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("3", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));

        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);

        PlaceOptionResponse option = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD, FoodSubCategory.KOREAN,
                false, userIds[0]).get(0);
        assertThat(option.getVoteCount()).isEqualTo(1);
        assertThat(option.isVotedByMe()).isTrue();
    }

    @Test
    void 체크를_해제하면_득표수가_다시_내려간다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("4", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));
        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);

        placeService.unvote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);

        PlaceOptionResponse option = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD, FoodSubCategory.KOREAN,
                false, userIds[0]).get(0);
        assertThat(option.getVoteCount()).isEqualTo(0);
        assertThat(option.isVotedByMe()).isFalse();
    }

    @Test
    void 이름이_같아도_id가_다르면_따로_집계된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("5", userIds);
        stubFoodSearch("한식", List.of(document("1", "국밥집"), document("2", "국밥집")));

        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);

        List<PlaceOptionResponse> options = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD,
                FoodSubCategory.KOREAN, false, userIds[0]);
        assertThat(options.stream().filter(o -> o.getId().equals("1")).findFirst().orElseThrow().getVoteCount())
                .isEqualTo(1);
        assertThat(options.stream().filter(o -> o.getId().equals("2")).findFirst().orElseThrow().getVoteCount())
                .isEqualTo(0);
    }

    @Test
    void 음식점과_카페를_각각_독립적으로_체크할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("6", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));
        stubCafeSearch(List.of(document("9", "카페온도")));

        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);
        placeService.vote(room.getCode(), PlaceCategory.CAFE, "9", userIds[0]);

        assertThat(placeService.getPlaces(room.getCode(), PlaceCategory.FOOD, FoodSubCategory.KOREAN, false, userIds[0])
                .get(0).getVoteCount()).isEqualTo(1);
        assertThat(placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, false, userIds[0])
                .get(0).getVoteCount()).isEqualTo(1);
    }

    @Test
    void 득표_1등이_아니어도_방장이_확정할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("7", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집"), document("2", "역전국밥")));
        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);
        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[1]);

        placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "역전국밥", userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getConfirmedFoodPlaceName()).isEqualTo("역전국밥");
    }

    @Test
    void 음식점과_카페를_각각_독립적으로_확정할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("8", userIds);

        placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", userIds[0]);
        placeService.confirmPlace(room.getCode(), PlaceCategory.CAFE, "카페온도", userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getConfirmedFoodPlaceName()).isEqualTo("정든백반집");
        assertThat(updated.getConfirmedCafePlaceName()).isEqualTo("카페온도");
    }

    @Test
    void 방장이_아니면_확정할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("9", userIds);

        assertThatThrownBy(() -> placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", userIds[1]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void 역이_확정되기_전에는_가게를_확정할_수_없다() {
        User owner = userRepository.save(User.createAnonymous("place-owner-10"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("가게 확정 실패 테스트").mode(RoomMode.MIDPOINT).build());

        assertThatThrownBy(() -> placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ROOM_STATUS);
    }

    @Test
    void 확정을_취소하면_해당_카테고리만_다시_null이_된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("11", userIds);
        placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", userIds[0]);
        placeService.confirmPlace(room.getCode(), PlaceCategory.CAFE, "카페온도", userIds[0]);

        placeService.clearPlace(room.getCode(), PlaceCategory.FOOD, userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getConfirmedFoodPlaceName()).isNull();
        assertThat(updated.getConfirmedCafePlaceName()).isEqualTo("카페온도");
    }

    @Test
    void 가게_투표_기록이_있는_방도_삭제할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("12", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));
        placeService.vote(room.getCode(), PlaceCategory.FOOD, "1", userIds[0]);

        roomService.deleteRoom(room.getCode(), userIds[0]);

        assertThatThrownBy(() -> roomService.getRoom(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 자유_키워드로_검색하면_추천과_별개로_결과를_받는다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("17", userIds);
        stubFoodSearch("파스타", List.of(document("100", "파스타공방")));

        List<PlaceOptionResponse> result = placeService.searchCustomPlaces(room.getCode(), PlaceCategory.FOOD,
                "파스타", userIds[0]);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("파스타공방");
        assertThat(result.get(0).isCustom()).isTrue();
        assertThat(result.get(0).getVoteCount()).isEqualTo(0);
    }

    @Test
    void 이미_이_방에_추가된_곳은_검색_결과에서_빠진다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("17b", userIds);
        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("100").placeName("파스타공방").build(), userIds[0]);
        stubFoodSearch("파스타", List.of(document("100", "파스타공방"), document("101", "다른파스타집")));

        List<PlaceOptionResponse> result = placeService.searchCustomPlaces(room.getCode(), PlaceCategory.FOOD,
                "파스타", userIds[0]);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("101");
    }

    @Test
    void 역_풀에_있는_곳도_검색_결과에서_빠진다() {
        when(kakaoLocalClient.searchStationCoordinates(any()))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.0, 127.0)));
        stationRepository.save(Station.create("검색풀테스트역", "테스트선"));

        User ownerA = userRepository.save(User.createAnonymous("search-pool-owner-a"));
        Room roomA = roomService.createRoom(ownerA.getId(), RoomCreateRequest.builder()
                .name("검색풀 테스트 방A").mode(RoomMode.FIXED).fixedStationName("검색풀테스트역").build());
        participantService.join(roomA.getCode(), ownerA.getId(), "방장A");
        placeService.addCustomPlace(roomA.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("100").placeName("파스타공방").build(), ownerA.getId());

        User ownerB = userRepository.save(User.createAnonymous("search-pool-owner-b"));
        Room roomB = roomService.createRoom(ownerB.getId(), RoomCreateRequest.builder()
                .name("검색풀 테스트 방B").mode(RoomMode.FIXED).fixedStationName("검색풀테스트역").build());
        participantService.join(roomB.getCode(), ownerB.getId(), "방장B");

        stubFoodSearch("파스타", List.of(document("100", "파스타공방"), document("101", "다른파스타집")));

        List<PlaceOptionResponse> result = placeService.searchCustomPlaces(roomB.getCode(), PlaceCategory.FOOD,
                "파스타", ownerB.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("101");
    }

    @Test
    void 추천_탭에는_직접_추가한_곳이_섞이지_않는다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("18", userIds);
        stubFoodSearch("한식", List.of(document("1", "정든백반집")));

        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("100").placeName("파스타공방")
                .roadAddressName("서울 테스트구 테스트로 2").placeUrl("http://place.map.kakao.com/100").walkMinutes(5)
                .build(), userIds[0]);

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD,
                FoodSubCategory.KOREAN, false, userIds[0]);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
    }

    @Test
    void 직접추가_탭은_추천_검색_없이_직접_추가한_곳만_보여준다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("18b", userIds);

        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("100").placeName("파스타공방")
                .roadAddressName("서울 테스트구 테스트로 2").placeUrl("http://place.map.kakao.com/100").walkMinutes(5)
                .build(), userIds[0]);

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.FOOD,
                null, true, userIds[0]);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("100");
        assertThat(result.get(0).isCustom()).isTrue();
    }

    @Test
    void 같은_곳을_중복_추가해도_에러_없이_무시된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19", userIds);
        PlaceCustomAddRequest request = PlaceCustomAddRequest.builder()
                .category(PlaceCategory.CAFE).placeId("200").placeName("동네카페").build();

        placeService.addCustomPlace(room.getCode(), request, userIds[0]);
        placeService.addCustomPlace(room.getCode(), request, userIds[0]);

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, true,
                userIds[0]);
        assertThat(result).hasSize(1);
    }

    @Test
    void 직접_추가한_가게엔_본인_기준_addedByMe가_표시된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19b", userIds);
        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.CAFE).placeId("201").placeName("동네카페2").build(), userIds[0]);

        List<PlaceOptionResponse> asAdder = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, true,
                userIds[0]);
        List<PlaceOptionResponse> asOther = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, true,
                userIds[1]);

        assertThat(asAdder.get(0).isAddedByMe()).isTrue();
        assertThat(asOther.get(0).isAddedByMe()).isFalse();
    }

    @Test
    void 직접_추가한_사람은_가게를_삭제할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19c", userIds);
        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.CAFE).placeId("202").placeName("동네카페3").build(), userIds[0]);

        placeService.deleteCustomPlace(room.getCode(), PlaceCategory.CAFE, "202", userIds[0]);

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, true,
                userIds[0]);
        assertThat(result).isEmpty();
    }

    @Test
    void 추가한_사람이_아니면_삭제할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19d", userIds);
        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.CAFE).placeId("203").placeName("동네카페4").build(), userIds[0]);

        assertThatThrownBy(() -> placeService.deleteCustomPlace(room.getCode(), PlaceCategory.CAFE, "203", userIds[1]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CUSTOM_PLACE_ADDER);
    }

    @Test
    void 방장은_본인이_추가하지_않아도_삭제할_수_있다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19f", userIds);
        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.CAFE).placeId("204").placeName("동네카페5").build(), userIds[1]);

        placeService.deleteCustomPlace(room.getCode(), PlaceCategory.CAFE, "204", userIds[0]);

        List<PlaceOptionResponse> result = placeService.getPlaces(room.getCode(), PlaceCategory.CAFE, null, true,
                userIds[0]);
        assertThat(result).isEmpty();
    }

    @Test
    void 존재하지_않는_직접추가_가게를_삭제하려하면_예외가_발생한다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("19e", userIds);

        assertThatThrownBy(() -> placeService.deleteCustomPlace(room.getCode(), PlaceCategory.CAFE, "없는placeId", userIds[0]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CUSTOM_PLACE_NOT_FOUND);
    }

    @Test
    void 참여하지_않으면_가게를_직접_추가할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithStationConfirmed("20", userIds);
        User stranger = userRepository.save(User.createAnonymous("place-stranger-1"));
        PlaceCustomAddRequest request = PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("300").placeName("낯선가게").build();

        assertThatThrownBy(() -> placeService.addCustomPlace(room.getCode(), request, stranger.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 가게를_직접_추가하면_역_풀에도_쌓인다() {
        stationRepository.save(Station.create("풀테스트역1", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("pool-owner-1"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방1").mode(RoomMode.FIXED).fixedStationName("풀테스트역1").build());
        participantService.join(room.getCode(), owner.getId(), "방장1");

        placeService.addCustomPlace(room.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("500").placeName("풀테스트식당")
                .roadAddressName("서울 풀테스트구 1").placeUrl("http://place.map.kakao.com/500")
                .walkMinutes(3).comment("짜장면 뒤지게맛있음").build(), owner.getId());

        assertThat(stationPlacePoolRepository
                .existsByStationNameAndCategoryAndPlaceId("풀테스트역1", PlaceCategory.FOOD, "500")).isTrue();
    }

    @Test
    void 다른_방이_같은_역에서_추가한_곳이_풀_제안으로_보인다() {
        stationRepository.save(Station.create("풀테스트역2", "테스트선"));
        User ownerA = userRepository.save(User.createAnonymous("pool-owner-2a"));
        Room roomA = roomService.createRoom(ownerA.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방2A").mode(RoomMode.FIXED).fixedStationName("풀테스트역2").build());
        participantService.join(roomA.getCode(), ownerA.getId(), "방장2A");
        placeService.addCustomPlace(roomA.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("501").placeName("풀테스트식당2")
                .comment("여기 진짜 맛집").build(), ownerA.getId());

        User ownerB = userRepository.save(User.createAnonymous("pool-owner-2b"));
        Room roomB = roomService.createRoom(ownerB.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방2B").mode(RoomMode.FIXED).fixedStationName("풀테스트역2").build());
        participantService.join(roomB.getCode(), ownerB.getId(), "방장2B");

        List<PlaceOptionResponse> suggestions = placeService.getPoolSuggestions(roomB.getCode(), PlaceCategory.FOOD,
                ownerB.getId());

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getId()).isEqualTo("501");
        assertThat(suggestions.get(0).getComment()).isEqualTo("여기 진짜 맛집");
    }

    @Test
    void 풀_제안도_투표하면_득표수가_반영된다() {
        stationRepository.save(Station.create("풀테스트역2b", "테스트선"));
        User ownerA = userRepository.save(User.createAnonymous("pool-owner-2c"));
        Room roomA = roomService.createRoom(ownerA.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방2C").mode(RoomMode.FIXED).fixedStationName("풀테스트역2b").build());
        participantService.join(roomA.getCode(), ownerA.getId(), "방장2C");
        placeService.addCustomPlace(roomA.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("510").placeName("풀전용식당").build(), ownerA.getId());

        User ownerB = userRepository.save(User.createAnonymous("pool-owner-2d"));
        Room roomB = roomService.createRoom(ownerB.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방2D").mode(RoomMode.FIXED).fixedStationName("풀테스트역2b").build());
        participantService.join(roomB.getCode(), ownerB.getId(), "방장2D");

        // 방 B는 풀 제안("510")에 place_custom_option 없이도 바로 투표 가능
        placeService.vote(roomB.getCode(), PlaceCategory.FOOD, "510", ownerB.getId());

        List<PlaceOptionResponse> suggestions = placeService.getPoolSuggestions(roomB.getCode(), PlaceCategory.FOOD,
                ownerB.getId());

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getVoteCount()).isEqualTo(1);
        assertThat(suggestions.get(0).isVotedByMe()).isTrue();
    }

    @Test
    void 풀_제안은_이_방에_이미_추가된_곳이어도_계속_보인다() {
        stationRepository.save(Station.create("풀테스트역3", "테스트선"));
        User ownerA = userRepository.save(User.createAnonymous("pool-owner-3a"));
        Room roomA = roomService.createRoom(ownerA.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방3A").mode(RoomMode.FIXED).fixedStationName("풀테스트역3").build());
        participantService.join(roomA.getCode(), ownerA.getId(), "방장3A");
        placeService.addCustomPlace(roomA.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("502").placeName("풀테스트식당3").build(), ownerA.getId());

        User ownerB = userRepository.save(User.createAnonymous("pool-owner-3b"));
        Room roomB = roomService.createRoom(ownerB.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방3B").mode(RoomMode.FIXED).fixedStationName("풀테스트역3").build());
        participantService.join(roomB.getCode(), ownerB.getId(), "방장3B");
        placeService.addCustomPlace(roomB.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("502").placeName("풀테스트식당3").build(), ownerB.getId());

        List<PlaceOptionResponse> suggestions = placeService.getPoolSuggestions(roomB.getCode(), PlaceCategory.FOOD,
                ownerB.getId());

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getId()).isEqualTo("502");
    }

    @Test
    void 같은_가게가_풀에_이미_있으면_새_코멘트로_덮어쓰지_않는다() {
        stationRepository.save(Station.create("풀테스트역4", "테스트선"));
        User ownerA = userRepository.save(User.createAnonymous("pool-owner-4a"));
        Room roomA = roomService.createRoom(ownerA.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방4A").mode(RoomMode.FIXED).fixedStationName("풀테스트역4").build());
        participantService.join(roomA.getCode(), ownerA.getId(), "방장4A");
        placeService.addCustomPlace(roomA.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("503").placeName("풀테스트식당4")
                .comment("원본 코멘트").build(), ownerA.getId());

        User ownerB = userRepository.save(User.createAnonymous("pool-owner-4b"));
        Room roomB = roomService.createRoom(ownerB.getId(), RoomCreateRequest.builder()
                .name("풀 테스트 방4B").mode(RoomMode.FIXED).fixedStationName("풀테스트역4").build());
        participantService.join(roomB.getCode(), ownerB.getId(), "방장4B");
        placeService.addCustomPlace(roomB.getCode(), PlaceCustomAddRequest.builder()
                .category(PlaceCategory.FOOD).placeId("503").placeName("풀테스트식당4")
                .comment("새 코멘트").build(), ownerB.getId());

        List<StationPlacePool> pool = stationPlacePoolRepository
                .findAllByStationNameAndCategory("풀테스트역4", PlaceCategory.FOOD);

        assertThat(pool).hasSize(1);
        assertThat(pool.get(0).getComment()).isEqualTo("원본 코멘트");
    }

    @Test
    void 포함하지_않은_카테고리는_확정할_수_없다() {
        when(kakaoLocalClient.searchStationCoordinates(any()))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.0, 127.0)));
        stationRepository.save(Station.create("카페제외역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("place-cat-1"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("음식점만 가는 모임").mode(RoomMode.FIXED).fixedStationName("카페제외역")
                .includesCafe(false).build());

        assertThatThrownBy(() -> placeService.confirmPlace(room.getCode(), PlaceCategory.CAFE, "아무카페", owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_INCLUDED);
    }

    @Test
    void 참여자가_2명_미만이면_가게를_확정할_수_없다() {
        when(kakaoLocalClient.searchStationCoordinates(any()))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.0, 127.0)));
        stationRepository.save(Station.create("혼자인역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("place-alone-1"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("방장 혼자 모임").mode(RoomMode.FIXED).fixedStationName("혼자인역").build());

        assertThatThrownBy(() -> placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ENOUGH_PARTICIPANTS);
    }
}
