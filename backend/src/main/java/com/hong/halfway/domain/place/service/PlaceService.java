package com.hong.halfway.domain.place.service;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.place.dto.PlaceCustomAddRequest;
import com.hong.halfway.domain.place.dto.PlaceOptionResponse;
import com.hong.halfway.domain.place.entity.FoodSubCategory;
import com.hong.halfway.domain.place.entity.PlaceCategory;
import com.hong.halfway.domain.place.entity.PlaceCustomOption;
import com.hong.halfway.domain.place.entity.PlaceOption;
import com.hong.halfway.domain.place.entity.PlaceVote;
import com.hong.halfway.domain.place.entity.StationPlacePool;
import com.hong.halfway.domain.place.repository.PlaceCustomOptionRepository;
import com.hong.halfway.domain.place.repository.PlaceVoteRepository;
import com.hong.halfway.domain.place.repository.StationPlacePoolRepository;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.station.client.KakaoLocalClient;
import com.hong.halfway.domain.station.client.dto.KakaoPlaceSearchResponse;
import com.hong.halfway.domain.station.entity.Station;
import com.hong.halfway.domain.station.repository.StationRepository;
import com.hong.halfway.domain.station.service.StationService;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    // 역 좌표 기준 가게 추천 검색 반경(m) 기본값, 참여자가 고를 수 있는 선택지
    private static final int DEFAULT_SEARCH_RADIUS_METERS = 300;
    private static final Set<Integer> ALLOWED_SEARCH_RADIUS_METERS = Set.of(300, 500, 1000);

    // 자유 키워드 직접 검색은 특정 가게를 찾는 용도라 추천보다 넓게 잡음(m)
    private static final int CUSTOM_SEARCH_RADIUS_METERS = 2000;

    // 카테고리(세부 카테고리)별 추천 후보 최대 수 (프론트에서 5개씩 "더보기"로 공개).
    // 반경이 넓을수록 더 많이 받아와야, 좁은 반경에서 이미 가까운 후보가 다 채워진 지역에서도
    // "반경을 넓혔는데 결과가 똑같다"는 문제 없이 실제로 더 먼 후보까지 보여줄 수 있음
    private static final Map<Integer, Integer> MAX_RESULTS_BY_RADIUS = Map.of(
            300, 15,
            500, 30,
            1000, 45);

    // 자유 키워드 직접 검색 결과 최대 수 (카카오 로컬 API가 한 번에 주는 최대치)
    private static final int CUSTOM_SEARCH_MAX_RESULTS = 15;

    // 카카오 로컬 API 카테고리 코드
    private static final String FOOD_CATEGORY_CODE = "FD6";
    private static final String CAFE_CATEGORY_CODE = "CE7";

    // 가게 확정에 필요한 최소 참여자 수. 혼자인 상태로 확정해버리는 것을 막기 위함 (FIXED 모드는 중간지점 계산 같은 자연스러운 인원 체크가 없어서 필요)
    private static final int MIN_PARTICIPANTS_TO_CONFIRM = 2;

    private static final Map<FoodSubCategory, String> FOOD_SUB_CATEGORY_KEYWORD = Map.of(
            FoodSubCategory.KOREAN, "한식",
            FoodSubCategory.CHINESE, "중식",
            FoodSubCategory.JAPANESE, "일식",
            FoodSubCategory.WESTERN, "양식",
            FoodSubCategory.PUB, "주점");

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PlaceVoteRepository placeVoteRepository;
    private final PlaceCustomOptionRepository placeCustomOptionRepository;
    private final StationPlacePoolRepository stationPlacePoolRepository;
    private final StationRepository stationRepository;
    private final StationService stationService;
    private final KakaoLocalClient kakaoLocalClient;

    /**
     * 확정된 역 주변 가게 후보와 득표 현황을 함께 조회. customOnly가 false면 세부 카테고리 추천 최대 15개(카카오 로컬 API 즉석 검색),
     * true면 참여자가 직접 검색해서 추가한 곳만 반환 ("직접추가" 탭). 직접 추가한 곳은 세부 카테고리가 없어 추천 목록과
     * 섞으면 모든 세부 카테고리 탭에 중복으로 보이므로 항상 분리해서 조회
     */
    public List<PlaceOptionResponse> getPlaces(String code, PlaceCategory category, FoodSubCategory subCategory,
            boolean customOnly, UUID requesterId) {
        int defaultMaxResults = MAX_RESULTS_BY_RADIUS.get(DEFAULT_SEARCH_RADIUS_METERS);
        return getPlaces(code, category, subCategory, customOnly, DEFAULT_SEARCH_RADIUS_METERS, defaultMaxResults, requesterId);
    }

    /**
     * 위와 동일하되, 추천 검색 반경(m)·받아올 최대 개수를 참여자가 고른 값으로 지정. customOnly=true일 땐 카카오 API를
     * 안 불러서 두 값 다 무시됨. maxResults는 프론트에서 "더보기"로 지금까지 펼친 개수만큼만 요청해서, 반경을 넓혀도
     * 실제로 펼쳐본 만큼만 카카오 API를 호출하게 함 — radiusMeters별 상한(MAX_RESULTS_BY_RADIUS)을 넘지는 못함
     */
    public List<PlaceOptionResponse> getPlaces(String code, PlaceCategory category, FoodSubCategory subCategory,
            boolean customOnly, int radiusMeters, int maxResults, UUID requesterId) {
        if (!ALLOWED_SEARCH_RADIUS_METERS.contains(radiusMeters)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        int effectiveMaxResults = Math.max(1, Math.min(maxResults, MAX_RESULTS_BY_RADIUS.get(radiusMeters)));
        Room room = findRoomByCode(code);
        if (room.getConfirmedStationName() == null) {
            return List.of();
        }

        Map<String, PlaceOption> optionsById = new LinkedHashMap<>();
        if (customOnly) {
            placeCustomOptionRepository.findAllByRoomAndCategory(room, category).stream()
                    .map(custom -> toPlaceOption(custom, subCategory))
                    .forEach(option -> optionsById.put(option.id(), option));
        } else {
            KakaoLocalClient.Coordinates origin = resolveStationCoordinates(room.getConfirmedStationName());
            if (origin == null) {
                throw new CustomException(ErrorCode.STATION_COORDINATES_NOT_READY);
            }
            List<PlaceOption> recommended = category == PlaceCategory.FOOD
                    ? searchFood(subCategory, origin, radiusMeters, effectiveMaxResults)
                    : searchCafe(origin, radiusMeters, effectiveMaxResults);
            recommended.forEach(option -> optionsById.put(option.id(), option));
        }

        Map<String, List<PlaceVote>> votesByPlaceId = placeVoteRepository.findAllByRoomAndCategory(room, category)
                .stream()
                .collect(Collectors.groupingBy(PlaceVote::getPlaceId));

        return optionsById.values().stream()
                .map(option -> toResponse(option, votesByPlaceId.getOrDefault(option.id(), List.of()), requesterId))
                .toList();
    }

    /**
     * 자유 키워드로 가게를 검색 (추천 목록과 별개, 직접 추가 전 미리보기).
     * 이미 이 방에 추가됐거나 역 풀("다른 모임에서 추가" 탭)에 있는 곳은 결과에서 제외(두 탭에 같은 곳이 중복으로 안 뜨게), 득표는 항상 0으로 내려감
     */
    public List<PlaceOptionResponse> searchCustomPlaces(String code, PlaceCategory category, String keyword,
            UUID requesterId) {
        Room room = findRoomByCode(code);
        if (room.getConfirmedStationName() == null) {
            return List.of();
        }
        KakaoLocalClient.Coordinates origin = resolveStationCoordinates(room.getConfirmedStationName());
        if (origin == null) {
            throw new CustomException(ErrorCode.STATION_COORDINATES_NOT_READY);
        }

        Set<String> excludedPlaceIds = placeCustomOptionRepository.findAllByRoomAndCategory(room, category).stream()
                .map(PlaceCustomOption::getPlaceId)
                .collect(Collectors.toCollection(HashSet::new));
        stationPlacePoolRepository.findAllByStationNameAndCategory(room.getConfirmedStationName(), category)
                .forEach(pool -> excludedPlaceIds.add(pool.getPlaceId()));

        String categoryCode = category == PlaceCategory.FOOD ? FOOD_CATEGORY_CODE : CAFE_CATEGORY_CODE;
        // 특정 가게 이름을 찾는 용도라 거리보다 키워드 일치도가 더 중요함
        List<PlaceOption> results = kakaoLocalClient
                .searchPlacesByKeyword(keyword, categoryCode, origin, CUSTOM_SEARCH_RADIUS_METERS, CUSTOM_SEARCH_MAX_RESULTS, "accuracy")
                .stream()
                .filter(document -> !excludedPlaceIds.contains(document.id()))
                .map(document -> toPlaceOption(document, category, null))
                .toList();

        return results.stream()
                .map(option -> toResponse(option, List.of(), requesterId))
                .toList();
    }

    /**
     * 같은 역에서 다른 방이 직접 추가했던 가게 목록 ("다른 모임에서 추가" 탭). 득표·확정까지 바로 가능한 완전한 후보로 취급
     */
    public List<PlaceOptionResponse> getPoolSuggestions(String code, PlaceCategory category, UUID requesterId) {
        Room room = findRoomByCode(code);
        if (room.getConfirmedStationName() == null) {
            return List.of();
        }

        Map<String, List<PlaceVote>> votesByPlaceId = placeVoteRepository.findAllByRoomAndCategory(room, category)
                .stream()
                .collect(Collectors.groupingBy(PlaceVote::getPlaceId));

        return stationPlacePoolRepository.findAllByStationNameAndCategory(room.getConfirmedStationName(), category).stream()
                .map(this::toPlaceOption)
                .map(option -> toResponse(option, votesByPlaceId.getOrDefault(option.id(), List.of()), requesterId))
                .toList();
    }

    /**
     * 검색 결과를 가게 후보로 추가. 참여자면 누구나 가능, 이미 추가된 placeId면 조용히 무시 (추천 후보 정립 방식과 동일하게 협업형)
     * 같은 역의 가게 풀에도 함께 적재해서 다음 모임에 제안되게 함 (이미 풀에 있으면 그대로 둠)
     */
    @Transactional
    public void addCustomPlace(String code, PlaceCustomAddRequest request, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        Room room = participant.getRoom();
        requireCategoryIncluded(room, request.getCategory());

        if (placeCustomOptionRepository.existsByRoomAndCategoryAndPlaceId(room, request.getCategory(), request.getPlaceId())) {
            log.info("[무시] 이미 추가된 가게 후보 - code: {}, category: {}", code, request.getCategory());
            return;
        }

        try {
            placeCustomOptionRepository.save(PlaceCustomOption.add(room, request.getCategory(), request.getPlaceId(),
                    request.getPlaceName(), request.getRoadAddressName(), request.getPlaceUrl(), request.getWalkMinutes(),
                    request.getComment(), participant));
            log.info("[완료] 가게 후보 직접 추가 - code: {}, category: {}", code, request.getCategory());
        } catch (DataIntegrityViolationException e) {
            log.info("[무시] 동시 요청으로 중복 추가됨 - code: {}, category: {}", code, request.getCategory());
        }

        addToStationPool(room.getConfirmedStationName(), request, participant.getUser());
    }

    /**
     * 직접 추가한 가게 삭제. 추가한 본인 또는 방장만 가능, 체크(투표) 기록도 함께 정리. 역 가게 풀(station_place_pool)은 그대로 둠
     */
    @Transactional
    public void deleteCustomPlace(String code, PlaceCategory category, String placeId, UUID userId) {
        Room room = findRoomByCode(code);
        PlaceCustomOption option = placeCustomOptionRepository.findByRoomAndCategoryAndPlaceId(room, category, placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_PLACE_NOT_FOUND));
        boolean isAdder = option.getParticipant() != null && option.getParticipant().getUser().getId().equals(userId);
        boolean isOwner = room.getOwner().getId().equals(userId);
        if (!isAdder && !isOwner) {
            throw new CustomException(ErrorCode.NOT_CUSTOM_PLACE_ADDER);
        }

        placeVoteRepository.deleteAllByRoomAndCategoryAndPlaceId(room, category, placeId);
        placeCustomOptionRepository.delete(option);
        log.info("[완료] 직접 추가한 가게 삭제 - code: {}, category: {}", code, category);
    }

    // 역 가게 풀에 적재. 이미 있으면 그대로 둠(최초 추가된 코멘트·등록자를 보존)
    private void addToStationPool(String stationName, PlaceCustomAddRequest request, User addedByUser) {
        if (stationName == null
                || stationPlacePoolRepository.existsByStationNameAndCategoryAndPlaceId(stationName, request.getCategory(), request.getPlaceId())) {
            return;
        }
        try {
            stationPlacePoolRepository.save(StationPlacePool.add(stationName, request.getCategory(), request.getPlaceId(),
                    request.getPlaceName(), request.getRoadAddressName(), request.getPlaceUrl(), request.getWalkMinutes(),
                    request.getComment(), addedByUser));
        } catch (DataIntegrityViolationException e) {
            log.info("[무시] 동시 요청으로 역 가게 풀에 중복 적재됨 - station: {}", stationName);
        }
    }

    /**
     * 가게 후보 체크 ("괜찮아요"). 이미 체크했으면 그대로 둠
     */
    @Transactional
    public void vote(String code, PlaceCategory category, String placeId, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        Room room = participant.getRoom();
        requireCategoryIncluded(room, category);
        if (placeVoteRepository.findByRoomAndParticipantAndCategoryAndPlaceId(room, participant, category, placeId)
                .isEmpty()) {
            placeVoteRepository.save(PlaceVote.check(room, participant, category, placeId));
        }
        log.info("[완료] 가게 후보 체크 - code: {}, category: {}", code, category);
    }

    /**
     * 가게 후보 체크 해제
     */
    @Transactional
    public void unvote(String code, PlaceCategory category, String placeId, UUID userId) {
        Participant participant = findMyParticipant(code, userId);
        placeVoteRepository.findByRoomAndParticipantAndCategoryAndPlaceId(participant.getRoom(), participant,
                        category, placeId)
                .ifPresent(placeVoteRepository::delete);
        log.info("[완료] 가게 후보 체크 해제 - code: {}, category: {}", code, category);
    }

    /**
     * 역 확정 후 카테고리별(음식점/카페) 가게 확정. 득표 1등이 아니어도 방장이 원하는 곳을 바로 확정할 수 있음 (방장만 가능)
     */
    @Transactional
    public void confirmPlace(String code, PlaceCategory category, String placeName, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }
        requireCategoryIncluded(room, category);
        if (participantRepository.countByRoom(room) < MIN_PARTICIPANTS_TO_CONFIRM) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_PARTICIPANTS);
        }

        room.choosePlace(category, placeName);
        log.info("[완료] 가게 확정 - code: {}, category: {}, place: {}", code, category, placeName);
    }

    /**
     * 카테고리별 가게 선택 취소, 카테고리 탐색으로 복귀 (방장만 가능)
     */
    @Transactional
    public void clearPlace(String code, PlaceCategory category, UUID requesterId) {
        Room room = findRoomByCode(code);
        requireOwner(room, requesterId);
        if (room.getStatus() != RoomStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_ROOM_STATUS);
        }
        requireCategoryIncluded(room, category);

        room.clearPlace(category);
        log.info("[완료] 가게 선택 취소 - code: {}, category: {}", code, category);
    }

    /**
     * 이 방의 가게 투표 기록과 직접 추가된 후보를 모두 삭제 (방 삭제 시 room 도메인에서 호출)
     */
    @Transactional
    public void deleteAllVotes(Room room) {
        placeVoteRepository.deleteAllByRoom(room);
        placeCustomOptionRepository.deleteAllByRoom(room);
    }

    // 음식점 세부 카테고리 키워드로 검색
    private List<PlaceOption> searchFood(FoodSubCategory subCategory, KakaoLocalClient.Coordinates origin,
            int radiusMeters, int maxResults) {
        String keyword = FOOD_SUB_CATEGORY_KEYWORD.getOrDefault(subCategory, "맛집");
        return kakaoLocalClient.searchPlacesByKeyword(keyword, FOOD_CATEGORY_CODE, origin, radiusMeters, maxResults, "distance")
                .stream()
                .map(document -> toPlaceOption(document, PlaceCategory.FOOD, subCategory, false))
                .toList();
    }

    // 카페 카테고리로 검색 (세부 카테고리 없음)
    private List<PlaceOption> searchCafe(KakaoLocalClient.Coordinates origin, int radiusMeters, int maxResults) {
        return kakaoLocalClient.searchPlacesByCategory(CAFE_CATEGORY_CODE, origin, radiusMeters, maxResults)
                .stream()
                .map(document -> toPlaceOption(document, PlaceCategory.CAFE, null, false))
                .toList();
    }

    // 자유 키워드 검색 결과는 추천과 구분해서 custom=true로 표시 (아직 추가되기 전 미리보기)
    private PlaceOption toPlaceOption(KakaoPlaceSearchResponse.Document document, PlaceCategory category,
            FoodSubCategory subCategory) {
        return toPlaceOption(document, category, subCategory, true);
    }

    private PlaceOption toPlaceOption(KakaoPlaceSearchResponse.Document document, PlaceCategory category,
            FoodSubCategory subCategory, boolean custom) {
        return new PlaceOption(document.id(), document.placeName(), category, subCategory,
                document.roadAddressName(), document.placeUrl(), calculateWalkMinutes(document.distance()), custom,
                null, null);
    }

    // 이미 저장된(직접 추가된) 가게 후보를 PlaceOption으로 변환
    private PlaceOption toPlaceOption(PlaceCustomOption custom, FoodSubCategory subCategory) {
        UUID addedByUserId = custom.getParticipant() != null ? custom.getParticipant().getUser().getId() : null;
        return new PlaceOption(custom.getPlaceId(), custom.getPlaceName(), custom.getCategory(), subCategory,
                custom.getRoadAddressName(), custom.getPlaceUrl(), custom.getWalkMinutes(), true, custom.getComment(),
                addedByUserId);
    }

    // 역 가게 풀에 쌓인 가게를 PlaceOption으로 변환 ("다른 모임에서 추가" 탭). 직접 추가한 곳과 동일하게 세부 카테고리 없음
    private PlaceOption toPlaceOption(StationPlacePool pool) {
        return new PlaceOption(pool.getPlaceId(), pool.getPlaceName(), pool.getCategory(), null,
                pool.getRoadAddressName(), pool.getPlaceUrl(), pool.getWalkMinutes(), true, pool.getComment(), null);
    }

    // 도보 시간 = ceil(직선거리(m) × 1.3 ÷ 80) (docs/DOMAIN.md 1절). 거리 정보가 없으면 null
    private Integer calculateWalkMinutes(String distanceMeters) {
        if (distanceMeters == null || distanceMeters.isBlank()) {
            return null;
        }
        try {
            double meters = Double.parseDouble(distanceMeters);
            return (int) Math.ceil(meters * 1.3 / 80);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 확정된 역의 좌표 조회. 아직 없으면 한 번 더 조회를 시도하고, 그래도 없으면 빈 값
    private KakaoLocalClient.Coordinates resolveStationCoordinates(String stationName) {
        Optional<Station> withCoordinates = findStationWithCoordinates(stationName);
        if (withCoordinates.isPresent()) {
            Station station = withCoordinates.get();
            return new KakaoLocalClient.Coordinates(station.getLat(), station.getLng());
        }

        stationService.ensureCoordinates(stationName);
        return findStationWithCoordinates(stationName)
                .map(station -> new KakaoLocalClient.Coordinates(station.getLat(), station.getLng()))
                .orElse(null);
    }

    private Optional<Station> findStationWithCoordinates(String stationName) {
        return stationRepository.findAllByName(stationName).stream()
                .filter(station -> station.getLat() != null && station.getLng() != null)
                .findFirst();
    }

    // 가게 후보 하나를 득표 현황과 함께 응답 DTO로 변환
    private PlaceOptionResponse toResponse(PlaceOption option, List<PlaceVote> votes, UUID requesterId) {
        boolean votedByMe = votes.stream()
                .anyMatch(vote -> vote.getParticipant().getUser().getId().equals(requesterId));
        return PlaceOptionResponse.from(option, votes.size(), votedByMe, requesterId);
    }

    // 이 방에서 선택 가능한 카테고리인지 확인, 아니면 예외 (FIXED 모드 등에서 음식점만/카페만 고를 때 사용)
    private void requireCategoryIncluded(Room room, PlaceCategory category) {
        boolean included = category == PlaceCategory.FOOD ? room.isIncludesFood() : room.isIncludesCafe();
        if (!included) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_INCLUDED);
        }
    }

    // 요청자가 방장인지 확인, 아니면 예외
    private void requireOwner(Room room, UUID requesterId) {
        if (!room.getOwner().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_OWNER);
        }
    }

    // 요청자 본인의 참여 정보 조회, 없으면 예외
    private Participant findMyParticipant(String code, UUID userId) {
        Room room = findRoomByCode(code);
        User user = userRepository.getReferenceById(userId);
        return participantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    // 코드로 방 조회, 없으면 예외
    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }
}
