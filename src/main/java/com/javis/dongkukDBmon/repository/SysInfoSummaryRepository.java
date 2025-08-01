package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
            "      WHERE DB_TYPE IN ('ORACLE', 'TIBERO','POSTGRESQL')" +
            "      and ID not in(521,520)) DBLIST \n" +
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
    List<SysInfoSummary> findLatestSummaryToday();

    @Query(value = "SELECT * FROM (" +
            "  SELECT * FROM TB_SYSINFO_SUMMARY " +
            "  WHERE HOSTNAME = :hostname " +
            "    AND TO_CHAR(CHECK_DATE, 'YYYY-MM-DD') = :date " +
            "  ORDER BY REG_TIME DESC" +
            ") WHERE ROWNUM = 1",
            nativeQuery = true)
    SysInfoSummary findLatestByHostnameAndDate(
            @Param("hostname") String hostname,
            @Param("date") String date
    );


    @Query("SELECT DISTINCT TO_CHAR(s.checkDate, 'YYYY-MM-DD') " +
            "FROM SysInfoSummary s " +
            "WHERE s.hostname = :hostname AND s.checkDate BETWEEN :start AND :end")
    List<String> findCollectedDatesBetween(@Param("hostname") String hostname,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);


}
