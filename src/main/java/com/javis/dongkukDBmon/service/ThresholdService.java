// ThresholdService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.repository.ThresholdRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.List;

@Slf4j
@Service
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;
    @PersistenceContext
    private EntityManager entityManager;

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
            existingThreshold.setImsiDel(threshold.getImsiDel());
            existingThreshold.setLastUpdateDate(new Date()); // ✅ 여기서 설정
            thresholdRepository.save(existingThreshold);
        });
    }
    @Transactional
    public int restoreExpiredImsiDel() {
        Date threeDaysAgo = Date.from(
                LocalDateTime.now().minusDays(3).atZone(ZoneId.systemDefault()).toInstant()
        );
        return thresholdRepository.restoreExpiredImsiDel(threeDaysAgo);
    }


    @Transactional
    public Threshold saveThreshold(Threshold threshold) {
        if (threshold.getId() == null) {
            threshold.setCreateDate(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        }
        threshold.setLastUpdateDate(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        return thresholdRepository.save(threshold);
    }

    @Transactional
    public void updateDefaultThreshold(Long id, Long defThresMb, String commt) {
        String sql = "UPDATE TB_DB_TBS_THRESHOLD SET DEF_THRES_MB = ?, LAST_UPDATE_DATE = SYSDATE, COMMT = ? WHERE ID = ?";
        entityManager.createNativeQuery(sql)
                .setParameter(1, defThresMb)
                .setParameter(2, commt)
                .setParameter(3, id)
                .executeUpdate();
    }

}

