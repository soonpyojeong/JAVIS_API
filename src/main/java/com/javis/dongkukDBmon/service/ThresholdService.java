// ThresholdService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.ThresholdWithUsageDto;
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


@Service
@Slf4j
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

    /** 11g 호환: JPA 표준으로 업서트 (id 없거나 모를 때도 안전) */
    @Transactional
    public Threshold saveOrUpdate(Threshold incoming) {
        Threshold target = null;

        if (incoming.getId() != null) {
            target = thresholdRepository.findById(incoming.getId()).orElse(null);
        }
        if (target == null) {
            target = thresholdRepository
                    .findByDbTypeAndDbNameAndTablespaceName(
                            incoming.getDbType(),
                            incoming.getDbName(),
                            incoming.getTablespaceName()
                    )
                    .orElse(null);
        }

        final Date now = new Date();

        if (target != null) {
            // UPDATE - 기본임계치는 건드리지 않음
            if (incoming.getThresMb() != null) {
                target.setThresMb(incoming.getThresMb());
            }
            if (incoming.getChkFlag() != null) {
                target.setChkFlag(incoming.getChkFlag());
            }
            if (incoming.getCommt() != null && !incoming.getCommt().isEmpty()) {
                target.setCommt(incoming.getCommt());
            }
            if (incoming.getImsiDel() != null || (incoming.getImsiDel() == null && target.getImsiDel() != null)) {
                target.setImsiDel(incoming.getImsiDel());
            }
            target.setLastUpdateDate(now);
            return thresholdRepository.save(target);

        } else {
            // INSERT - 기본임계치를 thresMb와 동일하게 세팅
            Threshold t = new Threshold();
            t.setDbType(incoming.getDbType());
            t.setDbName(incoming.getDbName());
            t.setTablespaceName(incoming.getTablespaceName());
            t.setThresMb(incoming.getThresMb());
            t.setDefThresMb(incoming.getThresMb()); // 신규 시 기본임계치 = 임계치
            t.setChkFlag(incoming.getChkFlag());
            t.setCommt(incoming.getCommt());
            t.setCreateDate(now);
            t.setLastUpdateDate(now);
            return thresholdRepository.save(t);
        }
    }


    /** 기존 코드 호환용: 내부적으로 업서트로 처리 */
    @Transactional
    public void save(Threshold threshold) {
        saveOrUpdate(threshold);
    }

    /** 3일 지난 IMSI_DEL 복구 (네이티브는 11g 호환) */
    @Transactional
    public int restoreExpiredImsiDel() {
        Date threeDaysAgo = new Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000);
        return thresholdRepository.restoreExpiredImsiDel(threeDaysAgo);
    }

    /** 기본 임계치 수정 (11g: SYSDATE 사용) */
    @Transactional
    public void updateDefaultThreshold(Long id, Long defThresMb, String commt) {
        String sql = "UPDATE TB_DB_TBS_THRESHOLD " +
                "   SET DEF_THRES_MB = ?, LAST_UPDATE_DATE = SYSDATE, COMMT = ? " +
                " WHERE ID = ?";
        entityManager.createNativeQuery(sql)
                .setParameter(1, defThresMb)
                .setParameter(2, commt)
                .setParameter(3, id)
                .executeUpdate();
    }

    /** 임시해제 */
    @Transactional
    public void releaseImsiDel(Long id) {
        String sql = "UPDATE TB_DB_TBS_THRESHOLD " +
                "   SET IMSI_DEL = NULL, LAST_UPDATE_DATE = SYSDATE " +
                " WHERE ID = ?";
        entityManager.createNativeQuery(sql)
                .setParameter(1, id)
                .executeUpdate();
    }

    public List<ThresholdWithUsageDto> findThresholdsWithUsage(String formattedTime) {
        return thresholdRepository.findWithUsage(formattedTime);
    }
}
