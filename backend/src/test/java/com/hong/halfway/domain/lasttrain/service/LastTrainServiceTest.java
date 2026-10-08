package com.hong.halfway.domain.lasttrain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.lasttrain.dto.LastTrainDayType;
import com.hong.halfway.domain.lasttrain.dto.LastTrainResponse;
import com.hong.halfway.domain.lasttrain.entity.TrainTimetable;
import com.hong.halfway.domain.lasttrain.repository.TrainTimetableRepository;
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
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LastTrainServiceTest {

    @Autowired
    private LastTrainService lastTrainService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationTravelTimeRepository stationTravelTimeRepository;

    @Autowired
    private TrainTimetableRepository trainTimetableRepository;

    private Station createStation(String name, String line, String frCode) {
        Station station = stationRepository.save(Station.create(name, line));
        if (frCode != null) {
            station.updateFrCode(frCode);
        }
        return station;
    }

    // TAGO 역 ID로만 시간표가 적재된 역(서울교통공사 외 노선) 생성
    private Station createTagoStation(String name, String line, String tagoStationId) {
        Station station = stationRepository.save(Station.create(name, line));
        station.updateTagoStationId(tagoStationId);
        return station;
    }

    private Room createFixedRoom(String suffix, String stationName) {
        User owner = userRepository.save(User.createAnonymous("lt-owner-" + suffix));
        return roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("막차 테스트").mode(RoomMode.FIXED).fixedStationName(stationName).build());
    }

    private void saveTrain(Station station, String trainNo, short weekTag, Short leaveMinute, Short arriveMinute) {
        trainTimetableRepository.save(TrainTimetable.create(
                station, trainNo, weekTag, (short) 1, arriveMinute, leaveMinute, "테스트시발역", "테스트종착역", "N"));
    }

    @Test
    void 역이_확정되지_않은_방이면_예외가_발생한다() {
        User owner = userRepository.save(User.createAnonymous("lt-owner-unconfirmed"));
        Room room = roomService.createRoom(owner.getId(), RoomCreateRequest.builder()
                .name("막차 테스트").mode(RoomMode.MIDPOINT).build());

        assertThatThrownBy(() -> lastTrainService.calculate(room.getCode(), "아무역", LastTrainDayType.WEEKDAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATION_NOT_CONFIRMED);
    }

    @Test
    void 등록되지_않은_귀가역이면_예외가_발생한다() {
        createStation("S테스트역nf", "테스트선nf", "900");
        Room room = createFixedRoom("nf", "S테스트역nf");

        assertThatThrownBy(() -> lastTrainService.calculate(room.getCode(), "없는역nf", LastTrainDayType.WEEKDAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATION_NOT_FOUND);
    }

    @Test
    void 약속역과_귀가역이_같으면_계산_없이_null을_반환한다() {
        createStation("S테스트역sm", "테스트선sm", "901");
        Room room = createFixedRoom("sm", "S테스트역sm");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "S테스트역sm", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isNull();
    }

    @Test
    void 경로에_시간표_없는_노선이_있으면_unsupported다() {
        Station s = createStation("S테스트역us", "테스트선us", "902");
        Station h = createStation("H테스트역us", "테스트선us", null);
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 5, (short) 0, s));
        Room room = createFixedRoom("us", "S테스트역us");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역us", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isTrue();
        assertThat(response.getLastDepartureTime()).isNull();
    }

    @Test
    void 두_구간을_거쳐_데드라인_안에_타는_가장_늦은_열차들로_막차를_역산한다() {
        Station s = createStation("S테스트역rd", "2호선", "903");
        Station m = createStation("M테스트역rd", "2호선", "904");
        Station h = createStation("H테스트역rd", "2호선", "905");
        stationTravelTimeRepository.save(StationTravelTime.create(s, m, (short) 6, (short) 0, s));
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 11, (short) 0, m));

        // M -> H: 가장 늦은 열차(Z, 1400 출발)가 뽑혀야 함
        saveTrain(m, "Y1", (short) 1, (short) 1250, null);
        saveTrain(h, "Y1", (short) 1, null, (short) 1300);
        saveTrain(m, "X1", (short) 1, (short) 1310, null);
        saveTrain(h, "X1", (short) 1, null, (short) 1320);
        saveTrain(m, "Z1", (short) 1, (short) 1400, null);
        saveTrain(h, "Z1", (short) 1, null, (short) 1410);

        // S -> M: 데드라인(1400) 안에 도착하는 것 중 가장 늦게 떠나는 건 P1(1350 출발, 1358 도착)
        saveTrain(s, "R1", (short) 1, (short) 1200, null);
        saveTrain(m, "R1", (short) 1, null, (short) 1206);
        saveTrain(s, "P1", (short) 1, (short) 1350, null);
        saveTrain(m, "P1", (short) 1, null, (short) 1358);
        saveTrain(s, "Q1", (short) 1, (short) 1395, null);
        saveTrain(m, "Q1", (short) 1, null, (short) 1403);

        Room room = createFixedRoom("rd", "S테스트역rd");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역rd", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isEqualTo("22:30");
    }

    @Test
    void 환승_구간은_환승_소요시간만큼_데드라인을_당겨서_역산한다() {
        Station s = createStation("S테스트역tr", "2호선", "906");
        Station ta = createStation("T테스트역tr", "2호선", "907");
        Station tb = createStation("T테스트역tr", "3호선", "908");
        Station h = createStation("H테스트역tr", "3호선", "909");

        stationTravelTimeRepository.save(StationTravelTime.create(s, ta, (short) 5, (short) 0, s));
        stationTravelTimeRepository.save(StationTravelTime.create(ta, tb, (short) 3, (short) 1, ta));
        stationTravelTimeRepository.save(StationTravelTime.create(s, tb, (short) 8, (short) 1, ta));
        stationTravelTimeRepository.save(StationTravelTime.create(tb, h, (short) 6, (short) 0, tb));
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 14, (short) 1, tb));

        // T(B) -> H: 하나뿐인 열차(1420 출발) 채택
        saveTrain(tb, "N1", (short) 1, (short) 1420, null);
        saveTrain(h, "N1", (short) 1, null, (short) 1430);

        // 환승 3분 적용 데드라인 1417. S -> T(A): 1417 안에 도착하는 A1(1410 출발)만 유효, B1은 도착이 늦어서 제외
        saveTrain(s, "A1", (short) 1, (short) 1410, null);
        saveTrain(ta, "A1", (short) 1, null, (short) 1416);
        saveTrain(s, "B1", (short) 1, (short) 1415, null);
        saveTrain(ta, "B1", (short) 1, null, (short) 1420);

        Room room = createFixedRoom("tr", "S테스트역tr");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역tr", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isEqualTo("23:30");
    }

    @Test
    void 요일구분에_맞는_시간표만_조회한다() {
        Station s = createStation("S테스트역wk", "2호선", "912");
        Station h = createStation("H테스트역wk", "2호선", "913");
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 5, (short) 0, s));

        // 토요일(weekTag=2) 시간표만 심어둠
        saveTrain(s, "SAT1", (short) 2, (short) 1430, null);
        saveTrain(h, "SAT1", (short) 2, null, (short) 1440);

        Room room = createFixedRoom("wk", "S테스트역wk");

        LastTrainResponse weekdayResponse = lastTrainService.calculate(room.getCode(), "H테스트역wk", LastTrainDayType.WEEKDAY);
        LastTrainResponse saturdayResponse = lastTrainService.calculate(room.getCode(), "H테스트역wk", LastTrainDayType.SATURDAY);

        assertThat(weekdayResponse.getLastDepartureTime()).isNull();
        assertThat(saturdayResponse.getLastDepartureTime()).isEqualTo("23:50");
    }

    @Test
    void TAGO_역_ID만_있어도_시간표가_있는_걸로_인정돼_막차를_계산한다() {
        Station s = createTagoStation("S테스트역tg", "신분당", "T001");
        Station h = createTagoStation("H테스트역tg", "신분당", "T002");
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 10, (short) 0, s));

        // TAGO 배치가 만드는 합성 trainNo 형태(T:노선:방향:종착역:순위)를 흉내냄 - 값 자체는 임의 문자열이면 충분
        saveTrain(s, "T:RTG:U:ETG:1", (short) 1, (short) 1420, null);
        saveTrain(h, "T:RTG:U:ETG:1", (short) 1, null, (short) 1430);

        Room room = createFixedRoom("tg", "S테스트역tg");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역tg", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isEqualTo("23:40");
    }

    @Test
    void 서울교통공사_노선과_TAGO_노선이_섞인_환승_경로도_막차를_계산한다() {
        Station s = createStation("S테스트역mx", "2호선", "920");
        Station ta = createStation("T테스트역mx", "2호선", "921");
        Station tb = createTagoStation("T테스트역mx", "신분당", "T003");
        Station h = createTagoStation("H테스트역mx", "신분당", "T004");

        stationTravelTimeRepository.save(StationTravelTime.create(s, ta, (short) 5, (short) 0, s));
        stationTravelTimeRepository.save(StationTravelTime.create(ta, tb, (short) 3, (short) 1, ta));
        stationTravelTimeRepository.save(StationTravelTime.create(s, tb, (short) 8, (short) 1, ta));
        stationTravelTimeRepository.save(StationTravelTime.create(tb, h, (short) 6, (short) 0, tb));
        stationTravelTimeRepository.save(StationTravelTime.create(s, h, (short) 14, (short) 1, tb));

        // T(B, 신분당) -> H(신분당): TAGO 합성 trainNo로 저장된 막차
        saveTrain(tb, "T:RTG:U:ETG:1", (short) 1, (short) 1420, null);
        saveTrain(h, "T:RTG:U:ETG:1", (short) 1, null, (short) 1430);

        // S(2호선) -> T(A, 2호선): 서울교통공사 실제 열차번호로 저장된 데드라인(1417) 안의 막차
        saveTrain(s, "A1", (short) 1, (short) 1410, null);
        saveTrain(ta, "A1", (short) 1, null, (short) 1416);

        Room room = createFixedRoom("mx", "S테스트역mx");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역mx", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isEqualTo("23:30");
    }

    @Test
    void 가장_빠른_경로에_그_요일_시간표가_없으면_차선_경로로_계산한다() {
        // 같은 이름의 역이 두 노선에 걸쳐 있는 환승역(S) 상황을 흉내냄: 9호선 쪽이 더 빠르지만 시간표가 비어있고,
        // 1호선 쪽은 더 느리지만 실제로 탈 수 있는 열차가 있는 경우
        Station sFast = createTagoStation("S테스트역fb", "9호선", "T901");
        Station sSlow = createStation("S테스트역fb", "1호선", "914");
        Station h = createStation("H테스트역fb", "9호선", "915");

        stationTravelTimeRepository.save(StationTravelTime.create(sFast, h, (short) 5, (short) 0, sFast));
        stationTravelTimeRepository.save(StationTravelTime.create(sSlow, h, (short) 10, (short) 0, sSlow));

        // 더 느린 1호선 쪽에만 실제 열차를 심어둠 (9호선 쪽은 데이터 자체가 없는 상황)
        saveTrain(sSlow, "W1", (short) 1, (short) 1420, null);
        saveTrain(h, "W1", (short) 1, null, (short) 1430);

        Room room = createFixedRoom("fb", "S테스트역fb");

        LastTrainResponse response = lastTrainService.calculate(room.getCode(), "H테스트역fb", LastTrainDayType.WEEKDAY);

        assertThat(response.isUnsupported()).isFalse();
        assertThat(response.getLastDepartureTime()).isEqualTo("23:40");
    }
}
