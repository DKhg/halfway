package com.hong.halfway.domain.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.room.entity.FairnessType;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ParticipantServiceTest {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private Room createRoom(String code) {
        User owner = userRepository.save(User.createAnonymous("owner-hash-" + code));
        Room room = Room.create(code, owner, "금요일 모임", RoomMode.MIDPOINT, FairnessType.MAX_MIN,
                LocalDate.now(), true, true);
        return roomRepository.save(room);
    }

    private Room createFixedRoom(String code) {
        User owner = userRepository.save(User.createAnonymous("fixed-owner-hash-" + code));
        Room room = Room.createFixed(code, owner, "고정 장소 모임", FairnessType.MAX_MIN,
                LocalDate.now(), true, true, "강남");
        return roomRepository.save(room);
    }

    private Room createSettlementOnlyRoom(String code) {
        User owner = userRepository.save(User.createAnonymous("settle-only-owner-hash-" + code));
        Room room = Room.createSettlementOnly(code, owner, "정산만 하기", LocalDate.now());
        return roomRepository.save(room);
    }

    @Test
    void 참여하면_닉네임이_저장되고_유저의_last_nickname도_갱신된다() {
        Room room = createRoom("code-1");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-1"));

        Participant participant = participantService.join(room.getCode(), participantUser.getId(), "지우");

        assertThat(participant.getNickname()).isEqualTo("지우");
        assertThat(participantUser.getLastNickname()).isEqualTo("지우");
    }

    @Test
    void 이미_참여한_방에_다시_참여하면_예외가_발생한다() {
        Room room = createRoom("code-2");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-2"));
        participantService.join(room.getCode(), participantUser.getId(), "지우");

        assertThatThrownBy(() -> participantService.join(room.getCode(), participantUser.getId(), "지우"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED);
    }

    @Test
    void 존재하지_않는_방_코드로_참여하면_예외가_발생한다() {
        User participantUser = userRepository.save(User.createAnonymous("p-hash-3"));

        assertThatThrownBy(() -> participantService.join("NOPE0000", participantUser.getId(), "지우"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 귀가역을_안_정하면_출발역과_같아진다() {
        Room room = createRoom("code-4");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-4"));
        participantService.join(room.getCode(), participantUser.getId(), "지우");
        ParticipantUpdateRequest request = ParticipantUpdateRequest.builder()
                .startStationName("강남역")
                .homeStationName(null)
                .build();

        Participant participant = participantService.updateMyInfo(room.getCode(), participantUser.getId(), request);

        assertThat(participant.getHomeStationName()).isEqualTo("강남역");
    }

    @Test
    void 방의_참여자_목록을_조회한다() {
        Room room = createRoom("code-6");
        User participant1 = userRepository.save(User.createAnonymous("p-hash-6-1"));
        User participant2 = userRepository.save(User.createAnonymous("p-hash-6-2"));
        participantService.join(room.getCode(), participant1.getId(), "지우");
        participantService.join(room.getCode(), participant2.getId(), "서연");

        List<Participant> participants = participantService.getParticipants(room.getCode());

        assertThat(participants).hasSize(2)
                .extracting(Participant::getNickname)
                .containsExactlyInAnyOrder("지우", "서연");
    }

    @Test
    void 존재하지_않는_방_코드로_참여자_목록을_조회하면_예외가_발생한다() {
        assertThatThrownBy(() -> participantService.getParticipants("NOPE0000"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 확정된_방에는_참여할_수_없다() {
        Room room = createRoom("code-7");
        room.confirm("강남");
        roomRepository.save(room);
        User participantUser = userRepository.save(User.createAnonymous("p-hash-7"));

        assertThatThrownBy(() -> participantService.join(room.getCode(), participantUser.getId(), "지우"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ALREADY_CONFIRMED);
    }

    @Test
    void FIXED_모드_방은_CONFIRMED_상태여도_참여할_수_있다() {
        Room room = createFixedRoom("fixed001");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-fixed-1"));

        Participant participant = participantService.join(room.getCode(), participantUser.getId(), "지우");

        assertThat(participant.getNickname()).isEqualTo("지우");
    }

    @Test
    void SETTLEMENT_ONLY_모드_방은_CONFIRMED_상태여도_참여할_수_있다() {
        Room room = createSettlementOnlyRoom("settle01");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-settle-1"));

        Participant participant = participantService.join(room.getCode(), participantUser.getId(), "지우");

        assertThat(participant.getNickname()).isEqualTo("지우");
    }

    @Test
    void 참여를_취소하면_다시_참여_정보를_찾을_수_없다() {
        Room room = createRoom("code-5");
        User participantUser = userRepository.save(User.createAnonymous("p-hash-5"));
        participantService.join(room.getCode(), participantUser.getId(), "지우");

        participantService.leave(room.getCode(), participantUser.getId());

        ParticipantUpdateRequest request = ParticipantUpdateRequest.builder()
                .startStationName("강남역")
                .build();
        assertThatThrownBy(() -> participantService.updateMyInfo(room.getCode(), participantUser.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
