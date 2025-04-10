package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.LiveChkMon;
import com.javis.dongkukDBmon.service.LiveChkMonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveChkMonRepository extends JpaRepository<LiveChkMon, Long> {
    // DB 이름과 날짜로 최신 테이블스페이스 데이터를 가져오기
    @Query(value = "select * from VW_TS_LIVE_CHK_MON where CHKMON is null",
            nativeQuery = true)
    List<LiveChkMon> findall();
}
