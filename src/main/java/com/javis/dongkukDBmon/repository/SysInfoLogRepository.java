package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysInfoLogRepository extends JpaRepository<SysInfoLog, Long> {
    List<SysInfoLog> findBySummaryIdAndLogType(Long summaryId, String logType);
    List<SysInfoLog> findBySummaryId(Long summaryId);
    // 추가: summaryId 여러 개로 한 번에 조회
    @Query("SELECT l FROM SysInfoLog l JOIN FETCH l.summary WHERE l.summary.id IN :summaryIds")
    List<SysInfoLog> findBySummaryIds(@Param("summaryIds") List<Long> summaryIds);

}
