package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.DbHealthStatusDto;
import com.javis.dongkukDBmon.service.DbStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DbDashboardController {

    private final DbStatusService dbStatusService;

    @GetMapping("/live-statuses")
    public ResponseEntity<List<DbHealthStatusDto>> getLiveStatuses() {
        return ResponseEntity.ok(dbStatusService.fetchLiveStatuses());
    }

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/db-live-status")
    public void postLiveStatus(@RequestBody Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/db-live-status", payload);
    }
}
