package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.DailyStatDto;
import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.model.Dailychk;
import com.javis.dongkukDBmon.model.TbDailychk;
import com.javis.dongkukDBmon.repository.DBListRepository;
import com.javis.dongkukDBmon.repository.DailyChkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DailyChkService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DBListRepository dbListRepository;

    @Autowired
    private DailyChkRepository dailyChkRepository;

    // DBType이 ORACLE 또는 TIBERO이고 dailyChk가 'Y'인 DB 목록을 dbType별로 분리하여 반환
    public Map<String, Map<String, List<String>>> getDbListForDailyChkByType() {
        List<DBList> dbList = dbListRepository.findAll();

        return dbList.stream()
                .filter(db -> "Y".equals(db.getDailyChk()) &&
                        ("ORACLE".equals(db.getDbType()) || "TIBERO".equals(db.getDbType())))
                .collect(Collectors.groupingBy(
                        DBList::getLoc, // 1차: 지역(loc) 기준 그룹핑
                        Collectors.groupingBy(
                                DBList::getDbType, // 2차: dbType 기준 그룹핑
                                Collectors.collectingAndThen(
                                        Collectors.mapping(
                                                DBList::getInstanceName,
                                                Collectors.toCollection(LinkedHashSet::new) // 중복 제거 + 순서 유지
                                        ),
                                        set -> new ArrayList<>(set) // ✅ 여기를 Function으로 감싸야 오류 없음
                                )
                        )
                ));
    }


    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<Dailychk> getDailyChkData(String instanceName) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return dailyChkRepository.findById_DbNameAndId_ChkDateBetween(instanceName);
    }
    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<TbDailychk> getTbDailyChkData(String instanceName) {
        return dailyChkRepository.findById_TbDbNameAndId_ChkDateBetween(instanceName);
    }

    public List<DailyStatDto> getDailyStatsLast7Days() {
        String sql = """
        SELECT *
        FROM (
            SELECT CHK_DATE, DB_TYPE, DB_DESC, DB_NAME, ACTIVE_SESSION, DAILY_ARCH_CNT
            FROM (
                SELECT CHK_DATE, 'ORACLE' AS DB_TYPE,
                       (SELECT DB_DESCRIPT FROM JAVIS.TB_JAVIS_DB_LIST WHERE INSTANCE_NAME = d.DB_NAME AND DB_TYPE = 'ORACLE' AND ROWNUM = 1) AS DB_DESC,
                       DB_NAME, ACTIVE_SESSION, DAILY_ARCH_CNT
                FROM JAVIS.TB_DB_DAILY_CHK d
                UNION ALL
                SELECT CHK_DATE, 'TIBERO' AS DB_TYPE,
                       (SELECT DB_DESCRIPT FROM JAVIS.TB_JAVIS_DB_LIST WHERE INSTANCE_NAME = t.DB_NAME AND DB_TYPE = 'TIBERO' AND ROWNUM = 1) AS DB_DESC,
                       DB_NAME, RUNNING_SESSION AS ACTIVE_SESSION, DAILY_ARCH_CNT
                FROM TB_DB_TIBERO_DAILY_CHK t
            )
            WHERE CHK_DATE >= TO_CHAR(SYSDATE - 7, 'YYYY/MM/DD')
              AND ACTIVE_SESSION IS NOT NULL
              AND DAILY_ARCH_CNT IS NOT NULL
            ORDER BY DAILY_ARCH_CNT DESC
        )
        WHERE ROWNUM <= 100
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DailyStatDto dto = new DailyStatDto();
            dto.setChkDate(rs.getString("CHK_DATE"));
            dto.setDbType(rs.getString("DB_TYPE"));
            dto.setDbDescript(rs.getString("DB_DESC"));
            dto.setDbName(rs.getString("DB_NAME"));
            dto.setActiveSession(rs.getInt("ACTIVE_SESSION"));
            dto.setDailyArchCnt(rs.getInt("DAILY_ARCH_CNT"));
            return dto;
        });
    }

    public List<DailyStatDto> getDailyStatsLast30Days() {
        String sql = """
        SELECT *
        FROM (
            SELECT CHK_DATE, DB_TYPE, DB_DESC, DB_NAME, ACTIVE_SESSION, DAILY_ARCH_CNT
            FROM (
                SELECT CHK_DATE, 'ORACLE' AS DB_TYPE,
                       (SELECT DB_DESCRIPT FROM JAVIS.TB_JAVIS_DB_LIST WHERE INSTANCE_NAME = d.DB_NAME AND DB_TYPE = 'ORACLE' AND ROWNUM = 1) AS DB_DESC,
                       DB_NAME, ACTIVE_SESSION, DAILY_ARCH_CNT
                FROM JAVIS.TB_DB_DAILY_CHK d
                UNION ALL
                SELECT CHK_DATE, 'TIBERO' AS DB_TYPE,
                       (SELECT DB_DESCRIPT FROM JAVIS.TB_JAVIS_DB_LIST WHERE INSTANCE_NAME = t.DB_NAME AND DB_TYPE = 'TIBERO' AND ROWNUM = 1) AS DB_DESC,
                       DB_NAME, RUNNING_SESSION AS ACTIVE_SESSION, DAILY_ARCH_CNT
                FROM TB_DB_TIBERO_DAILY_CHK t
            )
            WHERE CHK_DATE >= TO_CHAR(SYSDATE - 30, 'YYYY/MM/DD')
              AND ACTIVE_SESSION IS NOT NULL
              AND DAILY_ARCH_CNT IS NOT NULL
            ORDER BY DAILY_ARCH_CNT DESC
        )
        WHERE ROWNUM <= 100
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DailyStatDto dto = new DailyStatDto();
            dto.setChkDate(rs.getString("CHK_DATE"));
            dto.setDbType(rs.getString("DB_TYPE"));
            dto.setDbDescript(rs.getString("DB_DESC"));
            dto.setDbName(rs.getString("DB_NAME"));
            dto.setActiveSession(rs.getInt("ACTIVE_SESSION"));
            dto.setDailyArchCnt(rs.getInt("DAILY_ARCH_CNT"));
            return dto;
        });
    }


}
