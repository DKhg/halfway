package com.hong.halfway.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 방입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 정보가 없습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 방입니다."),
    ROOM_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 약속이라 더 이상 참여할 수 없어요."),
    NOT_ROOM_OWNER(HttpStatus.FORBIDDEN, "방장만 할 수 있어요."),
    STATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "등록되지 않은 역 이름이 있어요."),
    INVALID_ROOM_STATUS(HttpStatus.CONFLICT, "지금 상태에서는 할 수 없는 작업이에요."),
    CANDIDATES_NOT_READY(HttpStatus.BAD_REQUEST, "아직 후보를 계산할 수 없어요."),
    CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 후보예요."),
    STATION_COORDINATES_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "장소 주변 좌표를 아직 구하지 못했어요."),
    PLACE_STILL_CONFIRMED(HttpStatus.CONFLICT, "음식점·카페 확정을 먼저 취소해야 역을 취소할 수 있어요."),
    SETTLEMENT_NOT_READY(HttpStatus.CONFLICT, "음식점이나 카페 중 하나라도 확정된 뒤에 정산을 시작할 수 있어요."),
    SETTLEMENT_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 다른 사람이 정산을 시작했어요."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 시작된 정산이 없어요."),
    NOT_SETTLEMENT_PAYER(HttpStatus.FORBIDDEN, "결제자만 할 수 있어요."),
    NOT_SETTLEMENT_PAYER_OR_OWNER(HttpStatus.FORBIDDEN, "방장이나 결제자만 취소할 수 있어요."),
    SETTLEMENT_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 항목이에요."),
    FIXED_STATION_REQUIRED(HttpStatus.BAD_REQUEST, "장소를 직접 정하려면 역 이름을 입력해야 해요."),
    AT_LEAST_ONE_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "음식점·카페 중 하나는 선택해야 해요."),
    CATEGORY_NOT_INCLUDED(HttpStatus.BAD_REQUEST, "이 방에서는 선택하지 않는 카테고리예요."),
    NOT_ENOUGH_PARTICIPANTS(HttpStatus.BAD_REQUEST, "최소 2명 이상 모여야 장소를 확정할 수 있어요."),
    FIXED_ROOM_CANNOT_UNCONFIRM(HttpStatus.CONFLICT, "장소를 직접 정한 방은 역 확정을 취소할 수 없어요. 역 수정을 이용해 주세요."),
    ROOM_STATION_NOT_CONFIRMED(HttpStatus.CONFLICT, "역이 아직 확정되지 않아 막차를 계산할 수 없어요."),
    CUSTOM_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "직접 추가한 가게가 아니에요."),
    NOT_CUSTOM_PLACE_ADDER(HttpStatus.FORBIDDEN, "직접 추가한 사람이나 방장만 지울 수 있어요."),
    INVALID_RECEIPT_IMAGE(HttpStatus.BAD_REQUEST, "이미지 파일만 올릴 수 있어요."),
    RECEIPT_IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "사진 용량이 너무 커요. 10MB 이하로 올려주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
