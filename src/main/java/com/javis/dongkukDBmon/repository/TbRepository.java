// TbRepository.java

package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TbRepository extends JpaRepository<TiberoCap_Check_Mg, TiberoCapCheckMgId> {

    // DB 이름과 날짜로 최신 테이블스페이스 데이터를 가져오기
    @Query(value = "SELECT * " +
            "FROM VW_DB_CAP_CHECK_MG " +
            "WHERE DB_NAME = :dbName " +
            "AND CREATE_TIMESTAMP >= TO_DATE(TO_CHAR(TO_DATE(:formattedTimeLimit, 'yyyy/mm/dd HH24:MI:SS'), 'yyyy/mm/dd HH24') || ':00:00', 'yyyy/mm/dd HH24:MI:SS') " +
            "order by USED_RATE desc",
            nativeQuery = true)
    List<TiberoCap_Check_Mg> findTablespacesByDbName(
            @Param("dbName") String dbName,
            @Param("formattedTimeLimit") String formattedTimeLimit);

    @Query(value = "SELECT * " +
            "FROM VW_DB_CAP_CHECK_MG " +
            "WHERE DB_NAME = :dbName " +
            "AND TS_NAME = :tsName " +
            "AND CREATE_TIMESTAMP >= SYSDATE - 1 " +
            "ORDER BY CREATE_TIMESTAMP DESC",
            nativeQuery = true)
    List<TiberoCap_Check_Mg> findRecentDataForChat(
            @Param("dbName") String dbName,
            @Param("tsName") String tsName);


    // 모든 DB 이름 가져오기
    @Query("SELECT DISTINCT t.id.dbName FROM TiberoCap_Check_Mg t")
    List<String>findAllDbNames();
}
