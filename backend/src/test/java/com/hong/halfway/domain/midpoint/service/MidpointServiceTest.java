package com.hong.halfway.domain.midpoint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.midpoint.dto.MidpointResultResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointStatus;
import com.hong.halfway.domain.midpoint.dto.MidpointWithHomeResultResponse;
import com.hong.halfway.domain.midpoint.dto.MidpointWithHomeStatus;
import com.hong.halfway.domain.participant.dto.ParticipantUpdateRequest;
import com.hong.halfway.domain.participant.service.ParticipantService;
import com.hong.halfway.domain.room.dto.RoomCreateRequest;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.service.RoomService;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.entity.StationTravelTime;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.repository.StationTravelTimeRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.domain.user.service.UserService;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MidpointServiceTest {

    @Autowired
    private MidpointService midpointService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    private Room createRoom(String suffix) {
        User owner = userRepository.save(User.createAnonymous("mid-owner-" + suffix));
        return roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("중간지점 테스트").mode(RoomMode.MIDPOINT).build());
    }

    private void joinWithStation(Room room, String userHashSuffix, String nickname, String stationName) {
        User user = userRepository.save(User.createAnonymous("mid-user-" + userHashSuffix));
        participantService.join(room.getCode(), user.getId(), nickname);
        participantService.updateMyInfo(room.getCode(), user.getId(), ParticipantUpdateRequest.builder()
                .startStationName(stationName)
                .build());
    }

    private void joinWithStationAndHome(Room room, String userHashSuffix, String nickname, String stationName,
            String homeStationName) {
        User user = userRepository.save(User.createAnonymous("mid-user-" + userHashSuffix));
        participantService.join(room.getCode(), user.getId(), nickname);
        participantService.updateMyInfo(room.getCode(), user.getId(), ParticipantUpdateRequest.builder()
                .startStationName(stationName)
                .build());
        userService.updateHomeStation(user.getId(), homeStationName);
    }

    @Test
    void 출발역_입력한_참여자가_2명_미만이면_WAITING_상태다() {
        Room room = createRoom("1");
        joinWithStation(room, "1", "혼자", "A테스트역1");

        MidpointResultResponse result = midpointService.getMidpointResult(room.getCode());

        assertThat(result.getStatus()).isEqualTo(MidpointStatus.WAITING);
        assertThat(result.getReadyParticipantCount()).isEqualTo(1);
        assertThat(result.getCandidates()).isEmpty();
    }

    @Test
    void 등록되지_않은_역_이름이면_예외가_발생한다() {
        Room room = createRoom("2");
        joinWithStation(room, "2a", "참여자1", "없는역2");
        joinWithStation(room, "2b", "참여자2", "없는역2-2");

        assertThatThrownBy(() -> midpointService.getMidpointResult(room.getCode()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATION_NOT_FOUND);
    }

    @Test
    void 참여자_출발역_기준으로_중간지점_후보를_계산한다() {
        Station a = stationRepository.save(Station.create("A테스트역3", "테스트선"));
        Station b = stationRepository.save(Station.create("B테스트역3", "테스트선"));
        Station c = stationRepository.save(Station.create("C테스트역3", "테스트선"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, b, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(a, c, (short) 5, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, a, (short) 10, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, c, (short) 8, (short) 0));

        Room room = createRoom("3");
        joinWithStation(room, "3a", "참여자1", "A테스트역3");
        joinWithStation(room, "3b", "참여자2", "B테스트역3");

        MidpointResultResponse result = midpointService.getMidpointResult(room.getCode());

        assertThat(result.getStatus()).isEqualTo(MidpointStatus.READY);
        assertThat(result.getCandidates()).isNotEmpty();
        assertThat(result.getCandidates().get(0).getStationName()).isEqualTo("C테스트역3");
        assertThat(result.getCandidates().get(0).getMaxMinutes()).isEqualTo(8);
        assertThat(result.getCandidates().get(0).getTimes()).hasSize(2);
    }

    @Test
    void 참여자_전원이_귀가역을_등록하지_않으면_HOME_STATION_PENDING_상태다() {
        stationRepository.save(Station.create("D테스트역4", "테스트선4"));

        Room room = createRoom("4");
        joinWithStationAndHome(room, "4a", "참여자1", "A테스트역4", "D테스트역4");
        joinWithStation(room, "4b", "참여자2", "B테스트역4");

        MidpointWithHomeResultResponse result = midpointService.getMidpointResultWithHome(room.getCode());

        assertThat(result.getStatus()).isEqualTo(MidpointWithHomeStatus.HOME_STATION_PENDING);
        assertThat(result.getHomeStationSetCount()).isEqualTo(1);
        assertThat(result.getReadyParticipantCount()).isEqualTo(2);
        assertThat(result.getCandidates()).isEmpty();
    }

    @Test
    void 전원_귀가역_등록시_출발_귀가_왕복_기준으로_후보를_계산한다() {
        Station a = stationRepository.save(Station.create("A테스트역5", "테스트선5"));
        Station b = stationRepository.save(Station.create("B테스트역5", "테스트선5"));
        Station c = stationRepository.save(Station.create("C테스트역5", "테스트선5"));
        Station d = stationRepository.save(Station.create("D테스트역5", "테스트선5"));
        Station e = stationRepository.save(Station.create("E테스트역5", "테스트선5"));
        stationTravelTimeRepository.save(StationTravelTime.create(a, c, (short) 6, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(c, a, (short) 6, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(b, c, (short) 9, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(c, b, (short) 9, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(d, c, (short) 3, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(c, d, (short) 3, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(e, c, (short) 7, (short) 0));
        stationTravelTimeRepository.save(StationTravelTime.create(c, e, (short) 7, (short) 0));

        Room room = createRoom("5");
        joinWithStationAndHome(room, "5a", "참여자1", "A테스트역5", "D테스트역5");
        joinWithStationAndHome(room, "5b", "참여자2", "B테스트역5", "E테스트역5");

        MidpointWithHomeResultResponse result = midpointService.getMidpointResultWithHome(room.getCode());

        assertThat(result.getStatus()).isEqualTo(MidpointWithHomeStatus.READY);
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().get(0).getStationName()).isEqualTo("C테스트역5");
        // 참여자1: 출발 6분 + 귀가 3분, 참여자2: 출발 9분 + 귀가 7분 (구간별로 따로 내려감, 합산값은 응답에 없음)
        assertThat(result.getCandidates().get(0).getTimes())
                .extracting("departureMinutes", "homeMinutes")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(6, 3),
                        org.assertj.core.groups.Tuple.tuple(9, 7));
    }
}
