package com.hong.halfway.domain.lasttrain.controller;

import com.hong.halfway.domain.lasttrain.dto.LastTrainDayType;
import com.hong.halfway.domain.lasttrain.dto.LastTrainResponse;
import com.hong.halfway.domain.lasttrain.service.LastTrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{code}")
public class LastTrainController {

    private final LastTrainService lastTrainService;

    /**
     * 막차 계산. 약속역에서 입력한 귀가역까지, 고른 요일 구분(평일/토요일/휴일) 기준으로 몇 시까지 출발해야 하는지 조회
     */
    @GetMapping("/last-train")
    public ResponseEntity<LastTrainResponse> calculate(@PathVariable String code,
            @RequestParam String homeStationName, @RequestParam LastTrainDayType dayType) {
        return ResponseEntity.ok(lastTrainService.calculate(code, homeStationName, dayType));
    }
}
