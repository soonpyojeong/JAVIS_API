package com.javis.dongkukDBmon.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.service.ThresholdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/threshold")
public class ThresholdController {
    private static final Logger logger = LoggerFactory.getLogger(SmsHistController.class);

    private final ThresholdService thresholdService;

    @Autowired
    public ThresholdController(ThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @GetMapping("/all")
    public List<Threshold> getAllThresholds() {
        return thresholdService.getAllThresholds();
    }



    @PutMapping("/{id}")
    public ResponseEntity<Boolean> updateThreshold(@PathVariable Long id, @RequestBody Threshold thresholdDto) {
        Optional<Threshold> optionalThreshold = thresholdService.getThresholdById(id);
        if (optionalThreshold.isPresent()) {
            Threshold threshold = optionalThreshold.get();
            threshold.setThresMb(thresholdDto.getThresMb());

            thresholdService.save(threshold);
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.status(404).body(false);
    }


    @PostMapping("/save")
    public ResponseEntity<Threshold> saveThreshold(@RequestBody Threshold threshold) {
        // 요청 로깅
        logger.info("Received request to save threshold: {}", threshold);

        // 서비스 계층에서 데이터를 저장
        Threshold savedThreshold = thresholdService.saveThreshold(threshold);

        // 응답 로깅
        logger.info("Threshold successfully saved: {}", savedThreshold);

        return ResponseEntity.ok(savedThreshold);
    }
}

