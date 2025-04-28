package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.UserAlert;
import com.javis.dongkukDBmon.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping("/{userId}/alerts")
    public List<UserAlert> getUserAlerts(@PathVariable String userId) {
        return alertService.getUserAlerts(userId);
    }

    @PostMapping
    public ResponseEntity<?> createAlert(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");
        String message = (String) payload.get("message");
        List<String> users = (List<String>) payload.get("userIds");

        Alert alert = alertService.createAlert(type, message);
        alertService.notifyUsers(alert, users);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{alertId}/hide")
    public ResponseEntity<?> hideAlert(@RequestParam String userId, @PathVariable Long alertId) {
        alertService.markAsDeleted(userId, alertId);
        return ResponseEntity.ok().build();
    }
}
