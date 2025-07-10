package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlBatch;
import com.javis.dongkukDBmon.model.EtlJobLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface EtlJobLogRepository extends JpaRepository<EtlJobLog, Long> {
    // 배치 ID만 distinct하게 페이징 (성능 빠름)
    @Query("SELECT DISTINCT e.batchId FROM EtlJobLog e WHERE e.jobId = :jobId ORDER BY e.batchId DESC")
    Page<Long> findDistinctBatchIdsByJobId(@Param("jobId") Long jobId, Pageable pageable);

    // 해당 배치 ID 목록에 포함된 로그만 가져옴
    @Query(
            value = """
    SELECT * FROM (
      SELECT DISTINCT batch_id, ROWNUM AS rn
      FROM (
        SELECT batch_id FROM tb_etl_job_log
        WHERE job_id = :jobId
        GROUP BY batch_id
        ORDER BY batch_id DESC
      )
      WHERE ROWNUM <= :endRow
    )
    WHERE rn > :startRow
    """,
            nativeQuery = true
    )
    List<Long> findPagedBatchIdsByJobId(
            @Param("jobId") Long jobId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow
    );

    List<EtlJobLog> findByBatchIdIn(List<Long> batchIds); // ✅ 이 라인 추가
    // 🔹 특정 Job ID에 해당하는 모든 로그
    List<EtlJobLog> findByBatchId(Long batchId);
    // 🔹 특정 Batch ID에 대한 가장 최신 로그
    EtlJobLog findTop1ByBatchIdOrderByExecutedAtDesc(Long batchId);
    // 가장 마지막 로그 1건 조회 (jobId + sourceDbId 기준)
    @Query(value = """
    SELECT *
      FROM (
        SELECT * 
          FROM TB_ETL_JOB_LOG 
         WHERE BATCH_ID = :batchId
           AND SOURCE_DB_ID = :sourceDbId
         ORDER BY EXECUTED_AT DESC
      )
     WHERE ROWNUM = 1
""", nativeQuery = true)
    Optional<EtlJobLog> findTopByBatchIdAndSourceDbId(@Param("batchId") Long batchId, @Param("sourceDbId") Long sourceDbId);

    // 🔹 특정 Job의 가장 최근 로그 조회 (최신 배치 기준으로)
    @Query(value = """
    SELECT *
      FROM TB_ETL_JOB_LOG
     WHERE BATCH_ID = (
         SELECT BATCH_ID
           FROM (
               SELECT BATCH_ID
                 FROM TB_ETL_BATCH
                WHERE JOB_ID = :jobId
                ORDER BY STARTED_AT DESC
           )
         WHERE ROWNUM = 1
     )
     AND EXECUTED_AT = (
         SELECT MAX(EXECUTED_AT)
           FROM TB_ETL_JOB_LOG
          WHERE BATCH_ID = (
              SELECT BATCH_ID
                FROM (
                    SELECT BATCH_ID
                      FROM TB_ETL_BATCH
                     WHERE JOB_ID = :jobId
                     ORDER BY STARTED_AT DESC
                )
               WHERE ROWNUM = 1
          )
     )
""", nativeQuery = true)
    EtlJobLog findLatestLogByJobId(@Param("jobId") Long jobId);

    @Query(value = """
    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
      FROM TB_ETL_JOB_LOG
     WHERE BATCH_ID = :batchId
       AND RESULT = 'FAIL'
""", nativeQuery = true)
    int hasFailLogInBatch(@Param("batchId") Long batchId);

    @Query(value = """
    SELECT batch_id FROM (
        SELECT batch_id FROM TB_ETL_BATCH
        WHERE JOB_ID = :jobId
        ORDER BY STARTED_AT DESC
    ) WHERE ROWNUM = 1
""", nativeQuery = true)
    Long findLatestBatchIdByJobId(@Param("jobId") Long jobId);

    @Query(
            value = "SELECT * FROM TB_ETL_JOB_LOG WHERE JOB_ID = :jobId ORDER BY BATCH_ID DESC, EXECUTED_AT DESC",
            nativeQuery = true
    )
    List<EtlJobLog> findAllByJobIdOrderByBatchIdDescExecutedAtDesc(@Param("jobId") Long jobId);


}
