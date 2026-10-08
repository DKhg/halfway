package com.hong.halfway.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.service.PlaceService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.entity.StationTravelTime;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.vote.dto.StationCandidateResponse;
import com.hong.halfway.domain.vote.dto.VoteResultResponse;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VoteServiceTest {

    @Autowired
    private VoteService voteService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    private Room createRoomWithTwoParticipants(String suffix, UUID[] userIdHolder) {
        Station a = stationRepository.save(Station.create("A테스트역" + suffix, "테스트선"));
        Station b = stationRepository.save(Station.create("B테스트역" + suffix, "테스트선"));
        Station c = stationRepository.save(Station.create("C테스트역" + suffix, "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(a, c, (short) 5, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, c, (short) 8, (short) 0));

        User owner = userRepository.save(User.createAnonymous("vote-owner-" + suffix));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("투표 테스트").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), owner.getId(), "참여자1");
        participantService.updateMyInfo(room.getCode(), owner.getId(), ParticipantUpdateRequest.builder()
                .startStationName("A테스트역" + suffix).build());

        User guest = userRepository.save(User.createAnonymous("vote-guest-" + suffix));
        participantService.join(room.getCode(), guest.getId(), "참여자2");
        participantService.updateMyInfo(room.getCode(), guest.getId(), ParticipantUpdateRequest.builder()
                .startStationName("B테스트역" + suffix).build());

        userIdHolder[0] = owner.getId();
        userIdHolder[1] = guest.getId();
        return room;
    }

    @Test
    void 방장이_투표를_시작하면_후보가_고정되고_상태가_VOTING이_된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("1", userIds);

        voteService.startVoting(room.getCode(), userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.VOTING);

        VoteResultResponse result = voteService.getCandidates(room.getCode(), userIds[0]);
        assertThat(result.getCandidates()).isNotEmpty();
        assertThat(result.getCandidates().get(0).getStationName()).isEqualTo("C테스트역1");
    }

    @Test
    void 방장이_아니면_투표를_시작할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("2", userIds);

        assertThatThrownBy(() -> voteService.startVoting(room.getCode(), userIds[1]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void 이미_투표중이면_다시_시작할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("3", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);

        assertThatThrownBy(() -> voteService.startVoting(room.getCode(), userIds[0]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ROOM_STATUS);
    }

    @Test
    void 투표를_취소하면_후보가_지워지고_COLLECTING으로_복귀한다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("4", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);

        voteService.cancelVoting(room.getCode(), userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.COLLECTING);
        VoteResultResponse result = voteService.getCandidates(room.getCode(), userIds[0]);
        assertThat(result.getCandidates()).isEmpty();
    }

    @Test
    void 참여자가_후보를_체크하면_득표수가_올라간다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("5", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();

        voteService.vote(room.getCode(), candidateId, userIds[0]);

        StationCandidateResponse candidate = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0);
        assertThat(candidate.getVoteCount()).isEqualTo(1);
        assertThat(candidate.isVotedByMe()).isTrue();
    }

    @Test
    void 두_참여자가_같은_후보를_체크하면_그_후보가_1등이_된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("6", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();

        voteService.vote(room.getCode(), candidateId, userIds[0]);
        voteService.vote(room.getCode(), candidateId, userIds[1]);

        VoteResultResponse result = voteService.getCandidates(room.getCode(), userIds[0]);
        assertThat(result.getWinnerCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void 아무도_체크하지_않았으면_1등이_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("7", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);

        VoteResultResponse result = voteService.getCandidates(room.getCode(), userIds[0]);

        assertThat(result.getWinnerCandidateId()).isNull();
    }

    @Test
    void 방장이_후보를_확정하면_방이_CONFIRMED가_되고_역_이름이_저장된다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("9", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        StationCandidateResponse candidate = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0);

        voteService.confirmCandidate(room.getCode(), candidate.getId(), userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.CONFIRMED);
        assertThat(updated.getConfirmedStationName()).isEqualTo(candidate.getStationName());
    }

    @Test
    void 방장이_아니면_확정할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("10", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();

        assertThatThrownBy(() -> voteService.confirmCandidate(room.getCode(), candidateId, userIds[1]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void 투표중이_아니면_확정할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("11", userIds);

        assertThatThrownBy(() -> voteService.confirmCandidate(room.getCode(), 1L, userIds[0]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ROOM_STATUS);
    }

    @Test
    void 방장이_확정을_취소하면_VOTING으로_복귀하고_확정역이_지워진다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("12", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, userIds[0]);

        voteService.unconfirm(room.getCode(), userIds[0]);

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.VOTING);
        assertThat(updated.getConfirmedStationName()).isNull();
        // 후보·체크 기록은 그대로 남아있어야 함
        assertThat(voteService.getCandidates(room.getCode(), userIds[0]).getCandidates()).isNotEmpty();
    }

    @Test
    void 음식점이나_카페가_확정된_상태에서는_역_확정을_취소할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("12-1", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, userIds[0]);
        placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", userIds[0]);

        assertThatThrownBy(() -> voteService.unconfirm(room.getCode(), userIds[0]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_STILL_CONFIRMED);

        placeService.clearPlace(room.getCode(), PlaceCategory.FOOD, userIds[0]);
        voteService.unconfirm(room.getCode(), userIds[0]);
        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.VOTING);
    }

    @Test
    void 방장이_아니면_확정을_취소할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("13", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();
        voteService.confirmCandidate(room.getCode(), candidateId, userIds[0]);

        assertThatThrownBy(() -> voteService.unconfirm(room.getCode(), userIds[1]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void CONFIRMED가_아니면_확정을_취소할_수_없다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("14", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);

        assertThatThrownBy(() -> voteService.unconfirm(room.getCode(), userIds[0]))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ROOM_STATUS);
    }

    @Test
    void FIXED_모드_방은_확정_취소를_할_수_없다() {
        stationRepository.save(Station.create("FIXED취소테스트역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("fixed-vote-1"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("고정 모임").mode(RoomMode.FIXED).fixedStationName("FIXED취소테스트역").build());

        assertThatThrownBy(() -> voteService.unconfirm(room.getCode(), owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FIXED_ROOM_CANNOT_UNCONFIRM);
    }

    @Test
    void FIXED_모드_방장이_역을_수정할_수_있다() {
        stationRepository.save(Station.create("FIXED수정전역", "테스트선"));
        stationRepository.save(Station.create("FIXED수정후역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("fixed-vote-2"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("고정 모임").mode(RoomMode.FIXED).fixedStationName("FIXED수정전역").build());

        voteService.updateFixedStation(room.getCode(), "FIXED수정후역", owner.getId());

        Room updated = roomService.getRoom(room.getCode());
        assertThat(updated.getConfirmedStationName()).isEqualTo("FIXED수정후역");
    }

    @Test
    void 가게가_확정된_뒤에는_FIXED_역을_수정할_수_없다() {
        stationRepository.save(Station.create("FIXED가게역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("fixed-vote-3"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("고정 모임").mode(RoomMode.FIXED).fixedStationName("FIXED가게역").build());
        participantService.join(room.getCode(), owner.getId(), "방장");
        User guest = userRepository.save(User.createAnonymous("fixed-vote-3-guest"));
        participantService.join(room.getCode(), guest.getId(), "손님");
        placeService.confirmPlace(room.getCode(), PlaceCategory.FOOD, "정든백반집", owner.getId());

        assertThatThrownBy(() -> voteService.updateFixedStation(room.getCode(), "다른역", owner.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_STILL_CONFIRMED);
    }

    @Test
    void 체크를_해제하면_득표수가_다시_내려간다() {
        UUID[] userIds = new UUID[2];
        Room room = createRoomWithTwoParticipants("8", userIds);
        voteService.startVoting(room.getCode(), userIds[0]);
        Long candidateId = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0).getId();
        voteService.vote(room.getCode(), candidateId, userIds[0]);

        voteService.unvote(room.getCode(), candidateId, userIds[0]);

        StationCandidateResponse candidate = voteService.getCandidates(room.getCode(), userIds[0]).getCandidates().get(0);
        assertThat(candidate.getVoteCount()).isEqualTo(0);
        assertThat(candidate.isVotedByMe()).isFalse();
    }
}
