package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysInfoLogRepository extends JpaRepository<SysInfoLog, Long> {
    List<SysInfoLog> findBySummaryIdAndLogType(Long summaryId, String logType);
    List<SysInfoLog> findBySummaryId(Long summaryId);

}
