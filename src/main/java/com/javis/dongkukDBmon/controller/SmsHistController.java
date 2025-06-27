package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.SmsHistDto;
import com.javis.dongkukDBmon.model.SmsHist;
import com.javis.dongkukDBmon.service.SmsHistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@RestController
@RequestMapping("/api/sms")
public class SmsHistController {

    @Autowired
    private SmsHistService smsHistService;

    @GetMapping("/all")
    public List<SmsHistDto> getSmsHistories(@RequestParam int day) {
        log.info("Request received to fetch SMS histories for {} days", day);
        List<SmsHistDto> histories = smsHistService.getSmsHistories(day);
        log.info("Returning {} SMS histories", histories.size());
        return histories;
    }

    @PutMapping("/updateall")
    public ResponseEntity<Map<String, Object>> updateAllSmsHistories() {
        int updatedCount = smsHistService.updateAllSmsHistories();
        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        return ResponseEntity.ok(result);
    }
}
