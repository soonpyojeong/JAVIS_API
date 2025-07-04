package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.DbHealthStatusDto;
import com.javis.dongkukDBmon.service.DbStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DbDashboardController {

    private final DbStatusService dbStatusService;

    @GetMapping("/live-statuses")
    public ResponseEntity<List<DbHealthStatusDto>> getLiveStatuses() {
        return ResponseEntity.ok(dbStatusService.fetchLiveStatuses());
    }
}
