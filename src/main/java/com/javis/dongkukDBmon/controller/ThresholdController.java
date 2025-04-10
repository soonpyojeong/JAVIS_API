package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.TbsChkmon;
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
}

