package com.hong.halfway.domain.settlement.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SettlementResponse {

    private final Long payerParticipantId;

    private final String payerNickname;

    private final String payerBank;

    private final String payerAccountNumber;

    private final List<SettlementItemResponse> items;

    private final List<ParticipantTotalResponse> totals;
}
