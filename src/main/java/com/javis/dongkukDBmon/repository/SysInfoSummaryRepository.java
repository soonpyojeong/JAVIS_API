package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SysInfoSummaryRepository extends JpaRepository<SysInfoSummary, Long> {
    List<SysInfoSummary> findByHostnameAndCheckDate(String hostname, LocalDate checkDate);

    @Query(value = "SELECT * FROM (SELECT * FROM TB_SYSINFO_SUMMARY ORDER BY REG_TIME DESC) WHERE ROWNUM = 1", nativeQuery = true)
    SysInfoSummary findLatestSummary();

    @Query(value = "SELECT DBLIST.LOC, SYS.HOSTNAME \n" +
            "FROM (SELECT DISTINCT hostname FROM TB_SYSINFO_SUMMARY) SYS, \n" +
            "     (SELECT ID, LOC, ASSETS, DB_DESCRIPT, HOSTNAME, DB_NAME, INSTANCE_NAME, DB_TYPE \n" +
            "      FROM TB_JAVIS_DB_LIST \n" +
            "      WHERE DB_TYPE IN ('ORACLE', 'TIBERO')) DBLIST \n" +
            "WHERE SYS.hostname = DBLIST.DB_NAME", nativeQuery = true)
    List<Object[]> findDistinctHostnames();


    @Query(value = "SELECT * FROM (SELECT * FROM TB_SYSINFO_SUMMARY where hostname=? ORDER BY REG_TIME DESC) WHERE ROWNUM = 1", nativeQuery = true)
    SysInfoSummary findTopByHostnameOrderByCheckDateDesc(String hostname);

    @Query(value = "SELECT * FROM ( " +
            "SELECT * FROM TB_SYSINFO_SUMMARY " +
            "WHERE hostname = ? AND TRUNC(CHECK_DATE) = TO_DATE(?, 'YYYY-MM-DD') " +
            "ORDER BY REG_TIME DESC) " +
            "WHERE ROWNUM = 1", nativeQuery = true)
    SysInfoSummary findTopByHostnameAndDate(String hostname, String date);


    // ✅ 오늘 수집된 최신 Summary ID (정렬 기준: REG_TIME)
    @Query(value = "SELECT * FROM TB_SYSINFO_SUMMARY WHERE TRUNC(REG_TIME) = TRUNC(SYSDATE) ORDER BY REG_TIME DESC ", nativeQuery = true)
    SysInfoSummary findLatestSummaryToday();
}
