package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.AlertSummaryDto;
import com.javis.dongkukDBmon.repository.AlertSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertSummaryController {

    @Autowired
    private AlertSummaryRepository repo;

    @GetMapping("/summary")
    public Map<String, Object> getAlertSummary() {
        List<AlertSummaryDto> alerts = repo.getAlertSummary();
        String collectedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alerts);
        result.put("collectedAt", collectedAt);
        return result;
    }
}
