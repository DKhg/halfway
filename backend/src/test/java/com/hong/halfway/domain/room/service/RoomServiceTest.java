package com.hong.halfway.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.dto.RoomUpdateRequest;
import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.settlement.service.SettlementService;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.entity.StationTravelTime;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.vote.service.VoteService;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoteService voteService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    @Test
    void 방을_생성하면_방장과_코드가_설정되고_상태는_COLLECTING이다() {
        User owner = userRepository.save(User.createAnonymous("hash-1"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getCode()).hasSize(8);
        assertThat(room.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(room.getStatus()).isEqualTo(RoomStatus.COLLECTING);
    }

    @Test
    void 공평_기준을_안_정하면_MAX_MIN이_기본값이다() {
        User owner = userRepository.save(User.createAnonymous("hash-2"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getFairnessType()).isEqualTo(FairnessType.MAX_MIN);
    }

    @Test
    void 약속_날짜를_안_정하면_null로_생성된다() {
        User owner = userRepository.save(User.createAnonymous("hash-3"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getMeetingDate()).isNull();
    }

    @Test
    void 약속_날짜를_정하면_그대로_저장된다() {
        User owner = userRepository.save(User.createAnonymous("hash-4"));
        LocalDate meetingDate = LocalDate.now().plusDays(10);
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .meetingDate(meetingDate)
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getMeetingDate()).isEqualTo(meetingDate);
    }

    @Test
    void 코드로_방을_조회한다() {
        User owner = userRepository.save(User.createAnonymous("hash-5"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .build();
        Room created = roomService.createRoom(owner.getId(), request);

        Room found = roomService.getRoom(created.getCode());

        assertThat(found.getCode()).isEqualTo(created.getCode());
    }

    @Test
    void 존재하지_않는_코드로_조회하면_예외가_발생한다() {
        assertThatThrownBy(() -> roomService.getRoom("NOPE0000"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 방장인_방과_참여한_방이_합쳐져서_조회된다() {
        User me = userRepository.save(User.createAnonymous("hash-6"));
        User other = userRepository.save(User.createAnonymous("hash-7"));
        Room ownedRoom = roomService.createRoom(me.getId(), RoomCreateRequest.builder()
                .name("내가 만든 방").mode(RoomMode.MIDPOINT).build());
        Room joinedRoom = roomService.createRoom(other.getId(), RoomCreateRequest.builder()
                .name("참여한 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(joinedRoom.getCode(), me.getId(), "나");

        List<Room> myRooms = roomService.getMyRooms(me.getId());

        assertThat(myRooms).extracting(Room::getCode)
                .containsExactlyInAnyOrder(ownedRoom.getCode(), joinedRoom.getCode());
    }

    @Test
    void 방장이면서_참여자이기도_하면_한_번만_나온다() {
        User me = userRepository.save(User.createAnonymous("hash-8"));
        Room room = roomService.createRoom(me.getId(), RoomCreateRequest.builder()
                .name("내가 만든 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), me.getId(), "나");

        List<Room> myRooms = roomService.getMyRooms(me.getId());

        assertThat(myRooms).extracting(Room::getCode).containsExactly(room.getCode());
    }

    @Test
    void 방장이_공평_기준을_변경할_수_있다() {
        User owner = userRepository.save(User.createAnonymous("hash-13"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("기준 변경 테스트").mode(RoomMode.MIDPOINT).build());

        Room updated = roomService.updateRoom(room.getCode(), owner.getId(),
                RoomUpdateRequest.builder().fairnessType(FairnessType.SUM_MIN).build());

        assertThat(updated.getFairnessType()).isEqualTo(FairnessType.SUM_MIN);
    }

    @Test
    void 방장이_아니면_공평_기준을_변경하려할_때_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("hash-14"));
        User stranger = userRepository.save(User.createAnonymous("hash-15"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("기준 변경 시도될 방").mode(RoomMode.MIDPOINT).build());

        RoomUpdateRequest request = RoomUpdateRequest.builder().fairnessType(FairnessType.SUM_MIN).build();
        assertThatThrownBy(() -> roomService.updateRoom(room.getCode(), stranger.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void 방장이_약속_날짜를_정할_수_있다() {
        User owner = userRepository.save(User.createAnonymous("hash-16"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("날짜 변경 테스트").mode(RoomMode.MIDPOINT).build());
        LocalDate meetingDate = LocalDate.now().plusDays(5);

        Room updated = roomService.updateMeetingDate(room.getCode(), owner.getId(), meetingDate);

        assertThat(updated.getMeetingDate()).isEqualTo(meetingDate);
    }

    @Test
    void 방장이_약속_날짜를_다시_미정으로_되돌릴_수_있다() {
        User owner = userRepository.save(User.createAnonymous("hash-17"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("날짜 되돌리기 테스트").mode(RoomMode.MIDPOINT).meetingDate(LocalDate.now().plusDays(5)).build());

        Room updated = roomService.updateMeetingDate(room.getCode(), owner.getId(), null);

        assertThat(updated.getMeetingDate()).isNull();
    }

    @Test
    void 참여하지_않은_사람은_약속_날짜를_변경하려할_때_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("hash-18"));
        User stranger = userRepository.save(User.createAnonymous("hash-19"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("날짜 변경 시도될 방").mode(RoomMode.MIDPOINT).build());

        assertThatThrownBy(() -> roomService.updateMeetingDate(room.getCode(), stranger.getId(), LocalDate.now()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 방장이_아니어도_참여자면_약속_날짜를_변경할_수_있다() {
        User owner = userRepository.save(User.createAnonymous("hash-20"));
        User guest = userRepository.save(User.createAnonymous("hash-21"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("참여자 날짜 변경 테스트").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), guest.getId(), "참여자");
        LocalDate meetingDate = LocalDate.now().plusDays(3);

        Room updated = roomService.updateMeetingDate(room.getCode(), guest.getId(), meetingDate);

        assertThat(updated.getMeetingDate()).isEqualTo(meetingDate);
    }

    @Test
    void 정산이_시작되면_약속_날짜를_변경할_수_없다() {
        User owner = userRepository.save(User.createAnonymous("hash-22"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("정산 중 날짜 변경 시도").mode(RoomMode.SETTLEMENT_ONLY).build());
        participantService.join(room.getCode(), owner.getId(), "방장");
        settlementService.start(room.getCode(), owner.getId());

        assertThatThrownBy(() -> roomService.updateMeetingDate(room.getCode(), owner.getId(), LocalDate.now()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ROOM_STATUS);
    }

    @Test
    void 방장이_삭제하면_참여자와_함께_지워진다() {
        User owner = userRepository.save(User.createAnonymous("hash-9"));
        User guest = userRepository.save(User.createAnonymous("hash-10"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("삭제될 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), guest.getId(), "손님");

        roomService.deleteRoom(room.getCode(), owner.getId());

        assertThatThrownBy(() -> roomService.getRoom(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 방장이_아니면_삭제하려할_때_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("hash-11"));
        User stranger = userRepository.save(User.createAnonymous("hash-12"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("삭제_시도될_방").mode(RoomMode.MIDPOINT).build());

        assertThatThrownBy(() -> roomService.deleteRoom(room.getCode(), stranger.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_OWNER);
    }

    @Test
    void 투표_중인_방도_삭제할_수_있다() {
        Station a = stationRepository.save(Station.create("A삭제테스트역", "테스트선"));
        Station b = stationRepository.save(Station.create("B삭제테스트역", "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));

        User owner = userRepository.save(User.createAnonymous("hash-16"));
        User guest = userRepository.save(User.createAnonymous("hash-17"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("투표 중 삭제될 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), owner.getId(), "방장");
        participantService.updateMyInfo(room.getCode(), owner.getId(), ParticipantUpdateRequest.builder()
                .startStationName("A삭제테스트역").build());
        participantService.join(room.getCode(), guest.getId(), "손님");
        participantService.updateMyInfo(room.getCode(), guest.getId(), ParticipantUpdateRequest.builder()
                .startStationName("B삭제테스트역").build());
        voteService.startVoting(room.getCode(), false, owner.getId());
        Long candidateId = voteService.getCandidates(room.getCode(), owner.getId()).getCandidates().get(0).getId();
        voteService.vote(room.getCode(), candidateId, owner.getId());

        roomService.deleteRoom(room.getCode(), owner.getId());

        assertThatThrownBy(() -> roomService.getRoom(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void FIXED_모드로_방을_만들면_바로_CONFIRMED_상태이고_역이_확정된다() {
        stationRepository.save(Station.create("FIXED테스트역", "테스트선"));
        User owner = userRepository.save(User.createAnonymous("fixed-hash-1"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("고정 장소 모임")
                .mode(RoomMode.FIXED)
                .fixedStationName("FIXED테스트역")
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CONFIRMED);
        assertThat(room.getConfirmedStationName()).isEqualTo("FIXED테스트역");
    }

    @Test
    void FIXED_모드인데_역_이름이_없으면_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("fixed-hash-2"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("고정 장소 모임")
                .mode(RoomMode.FIXED)
                .build();

        assertThatThrownBy(() -> roomService.createRoom(owner.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FIXED_STATION_REQUIRED);
    }

    @Test
    void FIXED_모드인데_존재하지_않는_역이면_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("fixed-hash-3"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("고정 장소 모임")
                .mode(RoomMode.FIXED)
                .fixedStationName("존재하지않는역이름")
                .build();

        assertThatThrownBy(() -> roomService.createRoom(owner.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATION_NOT_FOUND);
    }

    @Test
    void 음식점과_카페를_둘_다_빼면_방_생성이_거부된다() {
        User owner = userRepository.save(User.createAnonymous("fixed-hash-4"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("카테고리 없는 모임")
                .mode(RoomMode.MIDPOINT)
                .includesFood(false)
                .includesCafe(false)
                .build();

        assertThatThrownBy(() -> roomService.createRoom(owner.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.AT_LEAST_ONE_CATEGORY_REQUIRED);
    }

    @Test
    void SETTLEMENT_ONLY_모드로_방을_만들면_역_없이_바로_CONFIRMED_상태다() {
        User owner = userRepository.save(User.createAnonymous("settle-only-hash-1"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("정산만 하기")
                .mode(RoomMode.SETTLEMENT_ONLY)
                .build();

        Room room = roomService.createRoom(owner.getId(), request);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CONFIRMED);
        assertThat(room.getConfirmedStationName()).isNull();
    }

    @Test
    void 호출할_때마다_다른_코드가_발급된다() {
        User owner = userRepository.save(User.createAnonymous("hash-4"));
        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("금요일 모임")
                .mode(RoomMode.MIDPOINT)
                .build();

        Room room1 = roomService.createRoom(owner.getId(), request);
        Room room2 = roomService.createRoom(owner.getId(), request);

        assertThat(room1.getCode()).isNotEqualTo(room2.getCode());
    }
}
