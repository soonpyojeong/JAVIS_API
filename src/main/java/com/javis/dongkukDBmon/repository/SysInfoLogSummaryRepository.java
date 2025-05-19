package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoLogSummary;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SysInfoLogSummaryRepository extends JpaRepository<SysInfoLogSummary, Long> {
    List<SysInfoLogSummary> findBySummaryId(Long summaryId);

    @Query("SELECT s FROM SysInfoSummary s WHERE s.hostname = :hostname AND FUNCTION('TO_CHAR', s.checkDate, 'yyyy/MM/dd') = :date ORDER BY s.checkDate DESC")
    SysInfoSummary findTopByHostnameAndDate(@Param("hostname") String hostname, @Param("date") String date);
}
