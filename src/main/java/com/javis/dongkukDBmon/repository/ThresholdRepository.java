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

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    Threshold findByDbNameAndTablespaceName(String dbName, String tablespaceName);
    @Modifying
    @Query("UPDATE Threshold t SET t.imsiDel = NULL WHERE t.imsiDel IS NOT NULL AND t.imsiDel < :expireDate")
    int restoreExpiredImsiDel(@Param("expireDate") Date expireDate);

    @Query(value = """
SELECT t.db_name, t.db_type, t.tablespace_name,
       t.thres_mb, t.def_thres_mb, t.imsi_del,
       s.total_size, s.used_size, s.free_size, s.used_rate
FROM VW_DB_CAP_CHECK_MG s, TB_DB_TBS_THRESHOLD t
WHERE s.CREATE_TIMESTAMP >= TO_DATE(TO_CHAR(TO_DATE(:formattedTime, 'yyyy/mm/dd HH24:MI:SS'), 'yyyy/mm/dd HH24') || ':00:00', 'yyyy/mm/dd HH24:MI:SS')
  AND t.db_name = s.db_name
  AND t.db_type = s.db_type
  AND t.TABLESPACE_NAME = s.TS_NAME
ORDER BY t.DB_NAME, s.used_rate DESC
""", nativeQuery = true)
    List<ThresholdWithUsageDto> findWithUsage(@Param("formattedTime") String formattedTime);


}
