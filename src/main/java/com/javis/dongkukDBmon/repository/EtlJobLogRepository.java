package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EtlJobLogRepository extends JpaRepository<EtlJobLog, Long> {
    List<EtlJobLog> findByJobId(Long jobId);
    List<EtlJobLog> findByJobIdAndResult(Long jobId, String result);
    @Query(value = "SELECT * FROM (SELECT * FROM tb_etl_job_log WHERE job_id = :jobId ORDER BY executed_at DESC) WHERE ROWNUM = 1", nativeQuery = true)
    EtlJobLog findLatestLogByJobId(@Param("jobId") Long jobId);
}
