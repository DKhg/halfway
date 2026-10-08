package com.hong.halfway.domain.settlement.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SettlementItemResponse {

    private final Long id;

    private final String name;

    private final int amount;

    private final List<Long> participantIds;

    private final List<String> participantNicknames;
}
