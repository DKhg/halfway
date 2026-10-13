package com.hong.halfway.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.user.dto.UserMeResponse;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ParticipantService participantService;

    @Test
    void 익명_사용자를_생성하면_발급된_토큰으로_조회할_수_있다() {
        String rawToken = userService.createAnonymousUser();

        User found = userRepository.findByTokenHash(AuthTokenUtils.hash(rawToken)).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getOauthProvider()).isNull();
    }

    @Test
    void 호출할_때마다_다른_토큰이_발급된다() {
        String token1 = userService.createAnonymousUser();
        String token2 = userService.createAnonymousUser();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void 기본_닉네임을_설정하면_내_정보_조회에_그대로_나온다() {
        User user = userRepository.save(User.createAnonymous("nickname-hash-1"));

        userService.updateNickname(user.getId(), "상계동감자");
        UserMeResponse me = userService.getMe(user.getId());

        assertThat(me.getNickname()).isEqualTo("상계동감자");
    }

    @Test
    void 계정을_삭제하면_방장인_방이_참여자와_함께_지워진다() {
        User owner = userRepository.save(User.createAnonymous("delete-hash-1"));
        User guest = userRepository.save(User.createAnonymous("delete-hash-2"));
        Room ownedRoom = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("삭제될 내 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(ownedRoom.getCode(), guest.getId(), "손님");

        userService.deleteMyAccount(owner.getId());

        assertThatThrownBy(() -> roomService.getRoom(ownedRoom.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
        assertThat(userRepository.findById(owner.getId())).isEmpty();
    }

    @Test
    void 계정을_삭제하면_참여자로만_있던_방은_남고_내_참여_기록만_빠진다() {
        User owner = userRepository.save(User.createAnonymous("delete-hash-3"));
        User guest = userRepository.save(User.createAnonymous("delete-hash-4"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("내가 참여만 한 방").mode(RoomMode.MIDPOINT).build());
        participantService.join(room.getCode(), guest.getId(), "손님");

        userService.deleteMyAccount(guest.getId());

        Room found = roomService.getRoom(room.getCode());
        assertThat(found.getCode()).isEqualTo(room.getCode());
        assertThat(userRepository.findById(guest.getId())).isEmpty();
    }
}
