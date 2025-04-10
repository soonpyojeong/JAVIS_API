// ThresholdService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.TbsChkmon;
import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.repository.ThresholdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;


@Service
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;

    @Autowired
    public ThresholdService(ThresholdRepository thresholdRepository) {
        this.thresholdRepository = thresholdRepository;
    }

    public List<Threshold> getAllThresholds() {
        return thresholdRepository.findAll();
    }

    public Optional<Threshold> getThresholdById(Long id) {
        return thresholdRepository.findById(id);
    }

    @Transactional
    public void save(Threshold threshold) {
        thresholdRepository.findById(threshold.getId()).ifPresent(existingThreshold -> {
            existingThreshold.setThresMb(threshold.getThresMb());
            thresholdRepository.save(existingThreshold);
        });
    }
}

