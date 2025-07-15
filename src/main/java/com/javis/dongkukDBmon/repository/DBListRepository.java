package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.Dto.DbListDto;
import com.javis.dongkukDBmon.model.DBList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DBListRepository extends JpaRepository<DBList, Long> {
    // DBList 엔티티에서 필요한 CRUD 기능을 제공
    @Modifying
    @Transactional
    @Query("UPDATE DBList d SET d.allChk = ?1")
    void updateAllChkStatusBulk(String status);


    // ✅ 추가: Oracle / Tibero + 수집 상태 DTO 조회
    @Query(value = "SELECT distinct INSTANCE_NAME as dbName, SIZE_CHK as sizeChk, DB_TYPE as dbType " +
            "FROM TB_JAVIS_DB_LIST " +
            "WHERE DB_TYPE IN ('ORACLE', 'TIBERO')", nativeQuery = true)
    List<DbListDto> findOracleAndTiberoDbList();
}

