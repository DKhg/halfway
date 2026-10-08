package com.hong.halfway.domain.station.controller;

import com.hong.halfway.domain.station.dto.StationOptionResponse;
import com.hong.halfway.domain.station.service.StationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    /**
     * 이름별로 노선을 묶은 전체 역 목록 조회 (출발역 자동완성용)
     */
    @GetMapping("/names")
    public ResponseEntity<List<StationOptionResponse>> getAllStationNames() {
        return ResponseEntity.ok(stationService.getAllStationNames());
    }
}
