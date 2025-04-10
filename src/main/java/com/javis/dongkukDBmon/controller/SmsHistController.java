package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.SmsHist;
import com.javis.dongkukDBmon.service.SmsHistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/sms")
public class SmsHistController {

    private static final Logger logger = LoggerFactory.getLogger(SmsHistController.class);

    @Autowired
    private SmsHistService smsHistService;

    @GetMapping("/all")
    public List<SmsHist> getSmsHistories(@RequestParam int day) {
        logger.info("Request received to fetch SMS histories for {} days", day);

        List<SmsHist> histories = smsHistService.getSmsHistories(day);

        logger.info("Returning {} SMS histories", histories.size());
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
