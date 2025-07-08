package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EtlBatchRepository extends JpaRepository<EtlBatch, Long> {

    // 🔹 특정 Job ID에 속한 전체 배치 목록
    List<EtlBatch> findByJobId(Long jobId);

    // 🔹 Optional로 가장 최신 배치 (JPA 문법용, 사용 시 주의)
    Optional<EtlBatch> findTop1ByJobIdOrderByStartedAtDesc(Long jobId);

    @Query("SELECT COUNT(DISTINCT b.batchId) FROM EtlBatch b WHERE b.jobId = :jobId")
    int countByJobId(@Param("jobId") Long jobId);


    @Query(value = """
        SELECT * FROM (
            SELECT * FROM TB_ETL_BATCH 
            WHERE JOB_ID = :jobId 
            ORDER BY STARTED_AT DESC
        ) WHERE ROWNUM = 1
    """, nativeQuery = true)
    Long findLatestBatchIdByJobId(Long jobId);

    @Query(value = """
        SELECT * FROM (
            SELECT * FROM TB_ETL_BATCH 
            WHERE JOB_ID = :jobId 
            ORDER BY STARTED_AT DESC
        ) WHERE ROWNUM = 1
    """, nativeQuery = true)
    EtlBatch findLatestBatchByJobId(Long jobId);
}
