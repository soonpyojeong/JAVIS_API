package com.javis.dongkukDBmon.repository;
import com.javis.dongkukDBmon.model.SmsdataHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SmsdataHistoryRepository extends JpaRepository<SmsdataHistory, Long> {

    // 최근 N일 이력 (Oracle 11g 기준, SYSDATE - N + 1)
    @Query(value =
            "SELECT * FROM SMSDATA_HISTORY WHERE INDATE >= TO_CHAR(TRUNC(SYSDATE) - :day + 1, 'YYYYMMDD')", nativeQuery = true)
    List<SmsdataHistory> findRecentHistories(@Param("day") int day);

    // 필요한 추가 쿼리(조건 검색 등)는 추후 확장
}