package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlJobRetryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlJobRetryLogRepository extends JpaRepository<EtlJobRetryLog, Long> {
    // 기본 save()만 사용해도 충분
}
