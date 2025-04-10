package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SmsHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SmsHistRepository extends JpaRepository<SmsHist, Long> {
    // nativeQuery로 Oracle SQL을 사용하여 VARCHAR2 필드의 날짜 비교
    @Query(value = "SELECT * FROM SMSDATA s WHERE s.inDate >= to_char(sysdate - :day, 'yyyymmdd')", nativeQuery = true)
    List<SmsHist> findSmsHistoriesAfterDays(@Param("day") int day);
}