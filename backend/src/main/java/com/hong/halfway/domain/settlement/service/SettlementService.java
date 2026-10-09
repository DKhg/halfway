package com.hong.halfway.domain.settlement.service;

import com.hong.halfway.domain.participant.entity.Participant;
import com.hong.halfway.domain.participant.repository.ParticipantRepository;
import com.hong.halfway.domain.place.service.PlaceService;
import com.hong.halfway.domain.room.entity.Room;
import com.hong.halfway.domain.room.entity.RoomMode;
import com.hong.halfway.domain.room.entity.RoomStatus;
import com.hong.halfway.domain.room.repository.RoomRepository;
import com.hong.halfway.domain.settlement.client.GeminiReceiptClient;
import com.hong.halfway.domain.settlement.dto.ParticipantTotalResponse;
import com.hong.halfway.domain.settlement.dto.ReceiptItemDraft;
import com.hong.halfway.domain.settlement.dto.SettlementItemRequest;
import com.hong.halfway.domain.settlement.dto.SettlementItemResponse;
import com.hong.halfway.domain.settlement.dto.SettlementResponse;
import com.hong.halfway.domain.settlement.entity.Settlement;
import com.hong.halfway.domain.settlement.entity.SettlementItem;
import com.hong.halfway.domain.settlement.entity.SettlementItemMember;
import com.hong.halfway.domain.settlement.repository.SettlementItemMemberRepository;
import com.hong.halfway.domain.settlement.repository.SettlementItemRepository;
import com.hong.halfway.domain.settlement.repository.SettlementRepository;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
public class SettlementService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final SettlementItemMemberRepository settlementItemMemberRepository;
    private final GeminiReceiptClient geminiReceiptClient;

    /**
     * 이 방에 정산이 이미 시작됐는지(결제자가 선점됐는지) 여부. 방 상세 조회에서 "정산" 탭 이동 가능 여부 판단에 사용
     */
    public boolean existsForRoom(Room room) {
        return settlementRepository.existsByRoom(room);
    }

    /**
     * 정산 시작(결제자 선점). 음식점·카페 중 하나라도 확정된 뒤부터 가능(둘 다 갈 필요는 없음), SETTLEMENT_ONLY 모드는 역·장소 조건 없이 바로 가능
     * 이미 시작됐으면 거절 (선착순, 동시 요청은 DB 유니크 제약으로 방지)
     */
    @Transactional
    public void start(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        boolean placeConfirmed = room.getStatus() == RoomStatus.CONFIRMED
                && (room.getConfirmedFoodPlaceName() != null || room.getConfirmedCafePlaceName() != null);
        if (!placeConfirmed && room.getMode() != RoomMode.SETTLEMENT_ONLY) {
            throw new CustomException(ErrorCode.SETTLEMENT_NOT_READY);
        }
        if (settlementRepository.existsByRoom(room)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_STARTED);
        }

        Participant payer = findMyParticipant(room, requesterId);
        try {
            settlementRepository.save(Settlement.start(room, payer));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_STARTED);
        }
        log.info("[완료] 정산 시작 - code: {}", code);
    }

    /**
     * 정산 항목·참여자별 총액 조회. 총액은 저장하지 않고 조회할 때마다 계산 (docs/DOMAIN.md 6절)
     */
    public SettlementResponse getSettlement(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        Settlement settlement = findSettlementByRoom(room);
        List<SettlementItem> items = settlementItemRepository.findAllBySettlementOrderByIdAsc(settlement);
        Map<Long, List<SettlementItemMember>> membersByItemId = settlementItemMemberRepository.findAllByItemIn(items)
                .stream()
                .collect(Collectors.groupingBy(member -> member.getItem().getId()));

        List<SettlementItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, membersByItemId.getOrDefault(item.getId(), List.of())))
                .toList();

        List<Participant> participants = participantRepository.findAllByRoom(room);
        List<ParticipantTotalResponse> totals = calculateTotals(settlement, items, membersByItemId, participants);

        return SettlementResponse.builder()
                .payerParticipantId(settlement.getPayer().getId())
                .payerNickname(settlement.getPayer().getNickname())
                .payerBank(settlement.getPayerBank())
                .payerAccountNumber(settlement.getPayerAccountNumber())
                .items(itemResponses)
                .totals(totals)
                .build();
    }

    /**
     * 결제자 계좌 정보 입력·수정 (결제자만 가능)
     */
    @Transactional
    public void updateAccount(String code, String bank, String accountNumber, UUID requesterId) {
        Settlement settlement = findSettlementByRoom(findRoomByCode(code));
        requirePayer(settlement, requesterId);

        settlement.updateAccount(bank, accountNumber);
        log.info("[완료] 정산 계좌 정보 입력 - code: {}", code);
    }

    /**
     * 정산 항목 추가 (결제자만 가능)
     */
    @Transactional
    public void addItem(String code, SettlementItemRequest request, UUID requesterId) {
        Room room = findRoomByCode(code);
        Settlement settlement = findSettlementByRoom(room);
        requirePayer(settlement, requesterId);

        List<Participant> members = findMembersInRoom(room, request.getParticipantIds());
        SettlementItem item = settlementItemRepository.save(
                SettlementItem.create(settlement, request.getName(), request.getAmount()));
        saveMembers(item, members);
        log.info("[완료] 정산 항목 추가 - code: {}, amount: {}", code, request.getAmount());
    }

    /**
     * 영수증 사진에서 상호명·합계 금액을 추출 (결제자만 가능). 이미지는 Gemini 호출 한 번에만 쓰고 저장하지 않음
     */
    public ReceiptItemDraft scanReceipt(String code, byte[] imageBytes, String contentType, UUID requesterId) {
        Settlement settlement = findSettlementByRoom(findRoomByCode(code));
        requirePayer(settlement, requesterId);
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_RECEIPT_IMAGE);
        }

        ReceiptItemDraft draft = geminiReceiptClient.extractReceipt(imageBytes, contentType);
        log.info("[완료] 영수증 인식 - code: {}, 인식됨: {}", code, draft.name() != null);
        return draft;
    }

    /**
     * 정산 항목 수정 (결제자만 가능)
     */
    @Transactional
    public void updateItem(String code, Long itemId, SettlementItemRequest request, UUID requesterId) {
        Room room = findRoomByCode(code);
        Settlement settlement = findSettlementByRoom(room);
        requirePayer(settlement, requesterId);
        SettlementItem item = findItemInSettlement(itemId, settlement);

        List<Participant> members = findMembersInRoom(room, request.getParticipantIds());
        item.update(request.getName(), request.getAmount());
        settlementItemMemberRepository.deleteAllByItem(item);
        // 유지되는 멤버의 경우 삭제와 재등록이 같은 (item_id, participant_id) 쌍이라, 플러시로 삭제를 먼저 반영해야 유니크 제약에 걸리지 않음
        settlementItemMemberRepository.flush();
        saveMembers(item, members);
        log.info("[완료] 정산 항목 수정 - code: {}, itemId: {}", code, itemId);
    }

    /**
     * 정산 항목 삭제 (결제자만 가능)
     */
    @Transactional
    public void deleteItem(String code, Long itemId, UUID requesterId) {
        Settlement settlement = findSettlementByRoom(findRoomByCode(code));
        requirePayer(settlement, requesterId);
        SettlementItem item = findItemInSettlement(itemId, settlement);

        settlementItemMemberRepository.deleteAllByItem(item);
        settlementItemRepository.delete(item);
        log.info("[완료] 정산 항목 삭제 - code: {}, itemId: {}", code, itemId);
    }

    /**
     * 정산 취소. 결제자 선점과 입력된 항목을 모두 지우고 처음부터 다시 시작할 수 있게 함
     * (방장 또는 결제자 본인만 가능: 결제자가 스스로 바로잡거나, 방장이 대신 정리할 수 있게)
     * SETTLEMENT_ONLY 모드는 정산이 방의 전부라 취소하면 방 자체를 삭제함
     */
    @Transactional
    public void cancel(String code, UUID requesterId) {
        Room room = findRoomByCode(code);
        Settlement settlement = findSettlementByRoom(room);
        boolean isOwner = room.getOwner().getId().equals(requesterId);
        boolean isPayer = settlement.getPayer().getUser().getId().equals(requesterId);
        if (!isOwner && !isPayer) {
            throw new CustomException(ErrorCode.NOT_SETTLEMENT_PAYER_OR_OWNER);
        }

        deleteSettlementAndItems(settlement);

        if (room.getMode() == RoomMode.SETTLEMENT_ONLY) {
            placeService.deleteAllVotes(room);
            participantRepository.deleteAll(participantRepository.findAllByRoom(room));
            roomRepository.delete(room);
            log.info("[완료] 정산 취소로 방 삭제 - code: {}", code);
            return;
        }

        log.info("[완료] 정산 취소 - code: {}", code);
    }

    /**
     * 이 방의 정산 기록을 모두 삭제 (방 삭제 시 room 도메인에서 호출)
     */
    @Transactional
    public void deleteAllForRoom(Room room) {
        settlementRepository.findByRoom(room).ifPresent(this::deleteSettlementAndItems);
    }

    private void deleteSettlementAndItems(Settlement settlement) {
        List<SettlementItem> items = settlementItemRepository.findAllBySettlementOrderByIdAsc(settlement);
        settlementItemMemberRepository.deleteAllByItemIn(items);
        settlementItemRepository.deleteAll(items);
        settlementRepository.delete(settlement);
    }

    // 항목별 1인 부담 계산 후 참여자별로 합산. 나머지는 결제자(멤버면)에게, 아니면 멤버 중 id가 가장 작은 사람에게
    private List<ParticipantTotalResponse> calculateTotals(Settlement settlement, List<SettlementItem> items,
            Map<Long, List<SettlementItemMember>> membersByItemId, List<Participant> participants) {
        Map<Long, Long> totalByParticipantId = participants.stream()
                .collect(Collectors.toMap(Participant::getId, participant -> 0L));

        for (SettlementItem item : items) {
            List<Participant> members = membersByItemId.getOrDefault(item.getId(), List.of()).stream()
                    .map(SettlementItemMember::getParticipant)
                    .toList();
            if (members.isEmpty()) {
                continue;
            }
            int share = item.getAmount() / members.size();
            int remainder = item.getAmount() - share * members.size();

            for (Participant member : members) {
                totalByParticipantId.merge(member.getId(), (long) share, Long::sum);
            }
            if (remainder > 0) {
                Participant remainderReceiver = members.stream()
                        .filter(member -> member.getId().equals(settlement.getPayer().getId()))
                        .findFirst()
                        .orElseGet(() -> members.stream().min(Comparator.comparing(Participant::getId)).orElseThrow());
                totalByParticipantId.merge(remainderReceiver.getId(), (long) remainder, Long::sum);
            }
        }

        return participants.stream()
                .map(participant -> ParticipantTotalResponse.builder()
                        .participantId(participant.getId())
                        .nickname(participant.getNickname())
                        .amount(totalByParticipantId.getOrDefault(participant.getId(), 0L))
                        .payer(participant.getId().equals(settlement.getPayer().getId()))
                        .build())
                .toList();
    }

    private SettlementItemResponse toItemResponse(SettlementItem item, List<SettlementItemMember> members) {
        return SettlementItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .amount(item.getAmount())
                .participantIds(members.stream().map(member -> member.getParticipant().getId()).toList())
                .participantNicknames(members.stream().map(member -> member.getParticipant().getNickname()).toList())
                .build();
    }

    private void saveMembers(SettlementItem item, List<Participant> members) {
        members.forEach(member -> settlementItemMemberRepository.save(SettlementItemMember.of(item, member)));
    }

    // 요청에 담긴 참여자 id들이 모두 이 방 소속인지 확인 후 조회, 하나라도 아니면 예외
    private List<Participant> findMembersInRoom(Room room, List<Long> participantIds) {
        List<Participant> members = participantRepository.findAllById(participantIds);
        boolean allInRoom = members.size() == participantIds.size()
                && members.stream().allMatch(member -> member.getRoom().getId().equals(room.getId()));
        if (!allInRoom) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return members;
    }

    // 요청자가 결제자인지 확인, 아니면 예외
    private void requirePayer(Settlement settlement, UUID requesterId) {
        if (!settlement.getPayer().getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_SETTLEMENT_PAYER);
        }
    }

    // 항목이 이 정산 소속인지 확인 후 조회, 아니면 예외
    private SettlementItem findItemInSettlement(Long itemId, Settlement settlement) {
        SettlementItem item = settlementItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_ITEM_NOT_FOUND));
        if (!item.getSettlement().getId().equals(settlement.getId())) {
            throw new CustomException(ErrorCode.SETTLEMENT_ITEM_NOT_FOUND);
        }
        return item;
    }

    // 요청자 본인의 참여 정보 조회, 없으면 예외
    private Participant findMyParticipant(Room room, UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return participantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    // 코드로 방 조회, 없으면 예외
    private Room findRoomByCode(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    // 방의 정산 조회, 없으면 예외
    private Settlement findSettlementByRoom(Room room) {
        return settlementRepository.findByRoom(room)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
    }
}
