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

    @Query(value = "SELECT DISTINCT hostname FROM TB_SYSINFO_SUMMARY", nativeQuery = true)
    List<String> findDistinctHostnames();

    @Query(value = "SELECT * FROM (SELECT * FROM TB_SYSINFO_SUMMARY where hostname=? ORDER BY REG_TIME DESC) WHERE ROWNUM = 1", nativeQuery = true)
    SysInfoSummary findTopByHostnameOrderByCheckDateDesc(String hostname);

    @Query(value = "SELECT * FROM (SELECT * FROM TB_SYSINFO_SUMMARY where hostname=? and CHECK_DATE=? ORDER BY REG_TIME DESC) WHERE ROWNUM = 1", nativeQuery = true)
    SysInfoSummary findTopByHostnameAndDate(String hostname, String date);

}
