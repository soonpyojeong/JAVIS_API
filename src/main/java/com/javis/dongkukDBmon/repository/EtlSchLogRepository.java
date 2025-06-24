// src/main/java/com/javis/dongkukDBmon/repository/EtlSchLogRepository.java
package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.TbEtlSchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface EtlSchLogRepository extends JpaRepository<TbEtlSchLog, Long> {

    @Query(value="""
        SELECT * FROM (
            SELECT l.*, ROWNUM rn
            FROM TB_ETL_SCH_LOG l
            WHERE (:fromDate IS NULL OR l.EXECUTED_AT >= TO_DATE(:fromDate, 'YYYY-MM-DD'))
              AND (:toDate IS NULL OR l.EXECUTED_AT < TO_DATE(:toDate, 'YYYY-MM-DD')+1)
              AND (:status IS NULL OR l.STATUS = :status)
              AND (:jobName IS NULL OR l.JOB_NAME LIKE '%' || :jobName || '%')
              AND (:message IS NULL OR l.MESSAGE LIKE '%' || :message || '%')
            ORDER BY l.EXECUTED_AT DESC
        ) WHERE rn BETWEEN :startRow AND :endRow
        """, nativeQuery = true)
    List<TbEtlSchLog> searchLogs(
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            @Param("status") String status,
            @Param("jobName") String jobName,
            @Param("message") String message,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow
    );

    // 네이티브 페이징
    @Query(
            value = """
        SELECT * FROM (
            SELECT l.*, ROWNUM rn
            FROM (
                SELECT * FROM TB_ETL_SCH_LOG
                WHERE SCHEDULE_ID = :scheduleId
                ORDER BY EXECUTED_AT DESC
            ) l
            WHERE ROWNUM <= :endRow
        )
        WHERE rn > :startRow
        """,
            nativeQuery = true
    )
    List<TbEtlSchLog> findLogsWithPaging(
            @Param("scheduleId") Long scheduleId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow
    );

    // 전체 카운트 쿼리도 필요!
    @Query(
            value = "SELECT COUNT(*) FROM TB_ETL_SCH_LOG WHERE SCHEDULE_ID = :scheduleId",
            nativeQuery = true
    )
    long countLogsByScheduleId(@Param("scheduleId") Long scheduleId);



}
