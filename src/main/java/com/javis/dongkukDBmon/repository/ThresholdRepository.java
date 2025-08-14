package com.javis.dongkukDBmon.repository;
import com.javis.dongkukDBmon.Dto.ThresholdWithUsageDto;
import com.javis.dongkukDBmon.model.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    Threshold findByDbNameAndTablespaceName(String dbName, String tablespaceName);
    @Modifying
    @Query("UPDATE Threshold t SET t.imsiDel = NULL WHERE t.imsiDel IS NOT NULL AND t.imsiDel < :expireDate")
    int restoreExpiredImsiDel(@Param("expireDate") Date expireDate);

    @Query(value = """
SELECT
  t.DB_NAME,
  t.DB_TYPE,
  t.TABLESPACE_NAME,
  t.THRES_MB,
  t.DEF_THRES_MB,
  t.IMSI_DEL,
  s.TOTAL_SIZE,
  s.USED_SIZE,
  s.FREE_SIZE,
  s.USED_RATE
FROM TB_DB_TBS_THRESHOLD t
JOIN (
  SELECT DB_NAME,
         TS_NAME,
         DB_TYPE,
         TOTAL_SIZE,
         USED_SIZE,
         FREE_SIZE,
         USED_RATE,
         CREATE_TIMESTAMP
  FROM (
    SELECT
      DB_NAME,
      TS_NAME,
      DB_TYPE,
      TOTAL_SIZE,
      USED_SIZE,
      FREE_SIZE,
      USED_RATE,
      CREATE_TIMESTAMP,
      ROW_NUMBER() OVER (
        PARTITION BY DB_NAME, TS_NAME
        ORDER BY CREATE_TIMESTAMP DESC
      ) AS RN
    FROM VW_DB_CAP_CHECK_MG
    WHERE CREATE_TIMESTAMP >= TO_DATE(
            TO_CHAR(TO_DATE(:formattedTime, 'YYYY/MM/DD HH24:MI:SS'),
                    'YYYY/MM/DD HH24') || ':00:00',
            'YYYY/MM/DD HH24:MI:SS'
          )
  )
  WHERE RN = 1
) s
  ON s.DB_NAME = t.DB_NAME
 AND s.DB_TYPE = t.DB_TYPE
 AND s.TS_NAME  = t.TABLESPACE_NAME
ORDER BY t.DB_NAME, s.USED_RATE DESC
""", nativeQuery = true)
    List<ThresholdWithUsageDto> findWithUsage(@Param("formattedTime") String formattedTime);

    // 👉 DB_TYPE + DB_NAME + TABLESPACE_NAME으로 단일 행 조회
    Optional<Threshold> findByDbTypeAndDbNameAndTablespaceName(String dbType, String dbName, String tablespaceName);


}

