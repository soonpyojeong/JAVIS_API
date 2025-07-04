package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.DBList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DbStatusRepository extends JpaRepository<DBList, Long> {

    @Query(value = """
        SELECT 
            d.DB_DESCRIPT AS name,
            NVL(CASE
                WHEN c.ERROR IS NOT NULL THEN '위험'
                WHEN c.MESSAGE LIKE '%경고%' THEN '주의'
                WHEN c.MESSAGE IS NOT NULL THEN '정상'
                ELSE '미수집'
            END, '미수집') AS status,
            TO_CHAR(c.CHK_DATE, 'YYYY-MM-DD HH24:MI:SS') AS chkDate,
            c.MESSAGE AS message,
            c.ERROR AS error
        FROM JAVIS.TB_JAVIS_DB_LIST d
        LEFT JOIN (
            SELECT *
            FROM JAVIS.TB_DB_LIVE_CHECK c
            WHERE (DB_TYPE, DB_NAME, DB_DESCRIPT, CHK_DATE) IN (
                SELECT DB_TYPE, DB_NAME, DB_DESCRIPT, MAX(CHK_DATE)
                FROM JAVIS.TB_DB_LIVE_CHECK
                GROUP BY DB_TYPE, DB_NAME, DB_DESCRIPT
            )
        ) c
        ON d.INSTANCE_NAME = c.DB_NAME
        AND d.DB_DESCRIPT = c.DB_DESCRIPT
        """, nativeQuery = true)
    List<Map<String, Object>> fetchDbHealthStatusRaw();
}
