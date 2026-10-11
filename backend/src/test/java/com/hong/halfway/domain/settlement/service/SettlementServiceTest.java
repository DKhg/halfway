package com.hong.halfway.domain.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.service.PlaceService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.settlement.dto.ParticipantTotalResponse;
import com.hong.halfway.domain.settlement.dto.SettlementItemRequest;
import com.hong.halfway.domain.settlement.dto.SettlementResponse;
import com.hong.halfway.domain.station.client.KakaoLocalClient;
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
class SettlementServiceTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private VoteService voteService;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    // 역·음식점·카페가 모두 확정된 방을 참여자 3명(닉네임 지정)으로 만들어 재사용
    private Room createSettlementReadyRoom(String suffix, String[] nicknames) {
        return createRoomWithConfirmedPlaces(suffix, nicknames, true, true);
    }

    // 음식점·카페 확정 여부를 선택적으로 조절할 수 있는 버전 (하나만 확정된 경우 테스트용)
    private Room createRoomWithConfirmedPlaces(String suffix, String[] nicknames, boolean confirmFood, boolean confirmCafe) {
        when(kakaoLocalClient.searchStationCoordinates(any()))
                .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(37.0, 127.0)));

        Station a = stationRepository.save(Station.create("A정산역" + suffix, "테스트선"));
        Station b = stationRepository.save(Station.create("B정산역" + suffix, "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));

        User owner = userRepository.save(User.createAnonymous("settle-owner-" + suffix));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("정산 테스트").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), owner.getId(), nicknames[0]);
        participantService.updateMyInfo(room.getCode(), owner.getId(), ParticipantUpdateRequest.builder()
                .startStationName("A정산역" + suffix).build());

        for (int i = 1; i < nicknames.length; i++) {
            User guest = userRepository.save(User.createAnonymous("settle-guest-" + suffix + "-" + i));
            participantService.join(room.getCode(), guest.getId(), nicknames[i]);
            participantService.updateMyInfo(room.getCode(), guest.getId(), ParticipantUpdateRequest.builder()
                    .startStationName("B정산역" + suffix).build());
        }

        voteService.startVoting(room.getCode(), owner.getId());
        Long candidateId = voteService.getCandidates(room.getCode(), owner.getId()).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, owner.getId());
        if (confirmFood) {
            placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", owner.getId());
        }
        if (confirmCafe) {
            placeService.confirmPlace(room.getCode(), PlaceCategory.CAFE, "카페온도", owner.getId());
        }

        return room;
    }

    private UUID userIdOf(Room room, String nickname) {
        Participant participant = participantService.getParticipants(room.getCode()).stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow();
        return participant.getUser().getId();
    }

    private Long participantIdOf(Room room, String nickname) {
        return participantService.getParticipants(room.getCode()).stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    void 정답_예시1_목업_시나리오() {
        Room room = createSettlementReadyRoom("1", new String[]{"지우", "민수", "서연"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");
        Long minsu = participantIdOf(room, "민수");
        Long seoyeon = participantIdOf(room, "서연");

        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("1차 고기·식사").amount(84000).participantIds(List.of(jiwoo, minsu, seoyeon)).build(), jiwooId);
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("1차 소주").amount(12000).participantIds(List.of(jiwoo, minsu)).build(), jiwooId);
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("2차 술집").amount(42000).participantIds(List.of(jiwoo, minsu)).build(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);

        assertThat(totalOf(response, "지우")).isEqualTo(55000);
        assertThat(totalOf(response, "민수")).isEqualTo(55000);
        assertThat(totalOf(response, "서연")).isEqualTo(28000);
    }

    @Test
    void 정답_예시2_나머지는_결제자에게() {
        Room room = createSettlementReadyRoom("2", new String[]{"지우", "민수", "서연"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");
        Long minsu = participantIdOf(room, "민수");
        Long seoyeon = participantIdOf(room, "서연");

        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("나머지테스트").amount(10000).participantIds(List.of(jiwoo, minsu, seoyeon)).build(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);

        assertThat(totalOf(response, "지우")).isEqualTo(3334);
        assertThat(totalOf(response, "민수")).isEqualTo(3333);
        assertThat(totalOf(response, "서연")).isEqualTo(3333);
    }

    @Test
    void 결제자가_항목에_없으면_나머지는_id가_가장_작은_멤버에게() {
        Room room = createSettlementReadyRoom("3", new String[]{"지우", "민수", "서연"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long minsu = participantIdOf(room, "민수");
        Long seoyeon = participantIdOf(room, "서연");

        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("지우없는항목").amount(10000).participantIds(List.of(minsu, seoyeon)).build(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        long minsuTotal = totalOf(response, "민수");
        long seoyeonTotal = totalOf(response, "서연");

        assertThat(minsuTotal + seoyeonTotal).isEqualTo(10000);
        // 민수 id가 더 작음(먼저 참여) -> 나머지는 민수에게
        assertThat(minsuTotal).isEqualTo(5000);
        assertThat(seoyeonTotal).isEqualTo(5000);
    }

    @Test
    void 역도_확정되지_않으면_정산을_시작할_수_없다() {
        User owner = userRepository.save(User.createAnonymous("settle-not-ready"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("정산 시작 실패 테스트").mode(RoomMode.MIDPOINT).build());

        assertThatThrownBy(() -> settlementService.start(room.getCode(), owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_READY);
    }

    @Test
    void 음식점_카페_둘다_확정되지_않으면_정산을_시작할_수_없다() {
        Room room = createRoomWithConfirmedPlaces("14", new String[]{"지우", "민수"}, false, false);
        UUID jiwooId = userIdOf(room, "지우");

        assertThatThrownBy(() -> settlementService.start(room.getCode(), jiwooId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_READY);
    }

    @Test
    void 음식점만_확정해도_정산을_시작할_수_있다() {
        Room room = createRoomWithConfirmedPlaces("15", new String[]{"지우", "민수"}, true, false);
        UUID jiwooId = userIdOf(room, "지우");

        settlementService.start(room.getCode(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        assertThat(response.getPayerNickname()).isEqualTo("지우");
    }

    @Test
    void 카페만_확정해도_정산을_시작할_수_있다() {
        Room room = createRoomWithConfirmedPlaces("16", new String[]{"지우", "민수"}, false, true);
        UUID jiwooId = userIdOf(room, "지우");

        settlementService.start(room.getCode(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        assertThat(response.getPayerNickname()).isEqualTo("지우");
    }

    @Test
    void 정산만_하기_모드는_역_장소_확정_없이_바로_정산을_시작할_수_있다() {
        User owner = userRepository.save(User.createAnonymous("settle-only-1"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("정산만 하기").mode(RoomMode.SETTLEMENT_ONLY).build());
        participantService.join(room.getCode(), owner.getId(), "지우");

        settlementService.start(room.getCode(), owner.getId());

        SettlementResponse response = settlementService.getSettlement(room.getCode(), owner.getId());
        assertThat(response.getPayerNickname()).isEqualTo("지우");
    }

    @Test
    void 정산만_하기_모드는_정산을_취소하면_방_자체가_삭제된다() {
        User owner = userRepository.save(User.createAnonymous("settle-only-2"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("정산만 하기 취소 테스트").mode(RoomMode.SETTLEMENT_ONLY).build());
        participantService.join(room.getCode(), owner.getId(), "지우");
        settlementService.start(room.getCode(), owner.getId());

        settlementService.cancel(room.getCode(), owner.getId());

        assertThatThrownBy(() -> roomService.getRoom(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 먼저_시작한_사람이_결제자가_되고_다른_사람은_시작할_수_없다() {
        Room room = createSettlementReadyRoom("4", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");
        UUID minsuId = userIdOf(room, "민수");

        settlementService.start(room.getCode(), jiwooId);

        assertThatThrownBy(() -> settlementService.start(room.getCode(), minsuId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_STARTED);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        assertThat(response.getPayerNickname()).isEqualTo("지우");
    }

    @Test
    void 결제자가_아니어도_항목을_추가할_수_있다() {
        Room room = createSettlementReadyRoom("5", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");
        UUID minsuId = userIdOf(room, "민수");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");

        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("민수가추가").amount(1000).participantIds(List.of(jiwoo)).build(), minsuId);

        assertThat(settlementService.getSettlement(room.getCode(), jiwooId).getItems()).hasSize(1);
    }

    @Test
    void 방_참여자가_아니면_항목을_추가할_수_없다() {
        Room room = createSettlementReadyRoom("5-2", new String[]{"지우", "민수"});
        Room otherRoom = createSettlementReadyRoom("5-3", new String[]{"다른방사람", "다른방사람2"});
        UUID jiwooId = userIdOf(room, "지우");
        UUID outsiderId = userIdOf(otherRoom, "다른방사람");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");

        assertThatThrownBy(() -> settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("몰래추가").amount(1000).participantIds(List.of(jiwoo)).build(), outsiderId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 다른_방_참여자_id로는_항목을_추가할_수_없다() {
        Room room = createSettlementReadyRoom("6", new String[]{"지우", "민수"});
        Room otherRoom = createSettlementReadyRoom("6-2", new String[]{"다른방사람", "다른방사람2"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long outsider = participantIdOf(otherRoom, "다른방사람");

        assertThatThrownBy(() -> settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("잘못된멤버").amount(1000).participantIds(List.of(outsider)).build(), jiwooId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 항목을_수정하면_금액과_멤버가_바뀐다() {
        Room room = createSettlementReadyRoom("7", new String[]{"지우", "민수", "서연"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");
        Long minsu = participantIdOf(room, "민수");
        Long seoyeon = participantIdOf(room, "서연");
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("원래항목").amount(10000).participantIds(List.of(jiwoo, minsu)).build(), jiwooId);
        Long itemId = settlementService.getSettlement(room.getCode(), jiwooId).getItems().get(0).getId();

        settlementService.updateItem(room.getCode(), itemId, SettlementItemRequest.builder()
                .name("수정된항목").amount(30000).participantIds(List.of(jiwoo, minsu, seoyeon)).build(), jiwooId);

        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("수정된항목");
        assertThat(response.getItems().get(0).getAmount()).isEqualTo(30000);
        assertThat(response.getItems().get(0).getParticipantIds()).containsExactlyInAnyOrder(jiwoo, minsu, seoyeon);
    }

    @Test
    void 항목을_삭제하면_목록에서_사라진다() {
        Room room = createSettlementReadyRoom("8", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("지울항목").amount(5000).participantIds(List.of(jiwoo)).build(), jiwooId);
        Long itemId = settlementService.getSettlement(room.getCode(), jiwooId).getItems().get(0).getId();

        settlementService.deleteItem(room.getCode(), itemId, jiwooId);

        assertThat(settlementService.getSettlement(room.getCode(), jiwooId).getItems()).isEmpty();
    }

    @Test
    void 방을_삭제하면_정산_기록도_함께_지워진다() {
        Room room = createSettlementReadyRoom("9", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");
        settlementService.start(room.getCode(), jiwooId);
        Long jiwoo = participantIdOf(room, "지우");
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("항목").amount(5000).participantIds(List.of(jiwoo)).build(), jiwooId);

        roomService.deleteRoom(room.getCode(), jiwooId);

        assertThatThrownBy(() -> roomService.getRoom(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 방장이_정산을_취소하면_결제자_선점과_항목이_모두_지워지고_다시_시작할_수_있다() {
        Room room = createSettlementReadyRoom("17", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");
        UUID minsuId = userIdOf(room, "민수");
        // 방장이 아닌 민수가 결제자가 된 상황
        settlementService.start(room.getCode(), minsuId);
        Long minsu = participantIdOf(room, "민수");
        settlementService.addItem(room.getCode(), SettlementItemRequest.builder()
                .name("항목").amount(5000).participantIds(List.of(minsu)).build(), minsuId);

        settlementService.cancel(room.getCode(), jiwooId);

        assertThatThrownBy(() -> settlementService.getSettlement(room.getCode(), jiwooId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);

        // 취소 후에는 다른 사람(방장)이 새로 시작할 수 있어야 함
        settlementService.start(room.getCode(), jiwooId);
        SettlementResponse response = settlementService.getSettlement(room.getCode(), jiwooId);
        assertThat(response.getPayerNickname()).isEqualTo("지우");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void 결제자_본인도_스스로_정산을_취소할_수_있다() {
        Room room = createSettlementReadyRoom("18", new String[]{"지우", "민수"});
        UUID minsuId = userIdOf(room, "민수");
        settlementService.start(room.getCode(), minsuId);

        settlementService.cancel(room.getCode(), minsuId);

        assertThatThrownBy(() -> settlementService.getSettlement(room.getCode(), minsuId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    void 방장도_결제자도_아니면_정산을_취소할_수_없다() {
        Room room = createSettlementReadyRoom("20", new String[]{"지우", "민수", "서연"});
        UUID minsuId = userIdOf(room, "민수");
        UUID seoyeonId = userIdOf(room, "서연");
        settlementService.start(room.getCode(), minsuId);

        assertThatThrownBy(() -> settlementService.cancel(room.getCode(), seoyeonId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_SETTLEMENT_PAYER_OR_OWNER);
    }

    @Test
    void 시작되지_않은_정산은_취소할_수_없다() {
        Room room = createSettlementReadyRoom("19", new String[]{"지우", "민수"});
        UUID jiwooId = userIdOf(room, "지우");

        assertThatThrownBy(() -> settlementService.cancel(room.getCode(), jiwooId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    private long totalOf(SettlementResponse response, String nickname) {
        return response.getTotals().stream()
                .filter(total -> total.getNickname().equals(nickname))
                .findFirst()
                .map(ParticipantTotalResponse::getAmount)
                .orElseThrow();
    }
}
