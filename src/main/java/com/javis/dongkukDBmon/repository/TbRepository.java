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
            "FROM VW_DB_CAP_THRES " +
            "WHERE DB_NAME = :dbName " +
            "order by USED_RATE desc",
            nativeQuery = true)
    List<TiberoCap_Check_Mg> findTablespacesByDbName(
            @Param("dbName") String dbName);

    @Query(value = "SELECT * " +
            "FROM VW_DB_CAP_THRES " +
            "WHERE DB_NAME = :dbName " +
            "AND TS_NAME = :tsName " +
            "ORDER BY CREATE_TIMESTAMP DESC",
            nativeQuery = true)
    List<TiberoCap_Check_Mg> findRecentDataForChat(
            @Param("dbName") String dbName,
            @Param("tsName") String tsName);


    // 모든 DB 이름 가져오기
    @Query(value ="SELECT DB_NAME FROM TB_DB_CAP_LIST t",
            nativeQuery = true)
    List<String>findAllDbNames();
}
