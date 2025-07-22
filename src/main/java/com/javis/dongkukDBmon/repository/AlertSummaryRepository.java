package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.Dto.AlertSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlertSummaryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<AlertSummaryDto> getAlertSummary() {
        String sql =
                "SELECT ALERT_TYPE, TIME, DB_DESC, MESSAGE, LVL, CHK_TYPE FROM ( "
                        + "SELECT 'INVALID' ALERT_TYPE, "
                        + "  TO_CHAR(CHK_DATE, 'YYYY/MM/DD HH24:MI:SS') AS TIME, " // <= TIME 컬럼 맞추기!
                        + "  (SELECT DISTINCT DB_DESCRIPT "
                        + "     FROM JAVIS.TB_JAVIS_DB_LIST "
                        + "    WHERE DB_TYPE=OBJ.DB_TYPE "
                        + "      AND INSTANCE_NAME=OBJ.DB_NAME "
                        + "      AND ROWNUM=1) AS DB_DESC, "
                        + "  object_type || ' Invalid Object ' || COUNT(1) || '건' AS MESSAGE, "
                        + "  'WARN' AS LVL, "
                        + "  'INVALID' CHK_TYPE "
                        + "FROM JAVIS.TB_DB_INVALID_OBJECT OBJ "
                        + "WHERE CHK_DATE >= SYSDATE - 2 "
                        + "GROUP BY DB_TYPE, DB_NAME, CHK_DATE, OBJECT_TYPE "
                        + "UNION ALL "
                        + "SELECT 'LOG' ALERT_TYPE, "
                        + "  TO_CHAR(TO_DATE(CHK_DATE || CHK_TIME, 'YYYYMMDDHH24MISS'), 'YYYY/MM/DD HH24:MI:SS') as TIME, "
                        + "  CASE "
                        + "    WHEN CHK_TYPE = 'TBS_SIZE' THEN "
                        + "      (SELECT DB_DESCRIPT "
                        + "       FROM ( "
                        + "         SELECT DB_DESCRIPT, DB_TYPE, INSTANCE_NAME "
                        + "           FROM (SELECT ID, DB_TYPE, INSTANCE_NAME, NVL(VIP,PUB_IP) IP, USERID, PW, PORT, DB_DESCRIPT, SMS_GROUP, "
                        + "                        DECODE(INS_TYPE, 'TAC', INSTANCE_NAME, DB_NAME) DB_NAME, "
                        + "                        ROW_NUMBER() OVER (PARTITION BY DB_TYPE, INSTANCE_NAME ORDER BY INSTANCE_NAME) CNT "
                        + "                   FROM TB_JAVIS_DB_LIST) "
                        + "          WHERE CNT = 1 "
                        + "       ) "
                        + "       WHERE DB_TYPE = EXLOG.DB_TYPE "
                        + "         AND INSTANCE_NAME = EXLOG.DB_DESCRIPT "
                        + "         AND ROWNUM = 1 "
                        + "      ) "
                        + "    ELSE EXLOG.DB_DESCRIPT "
                        + "  END AS DB_DESC, "
                        + "  ERROR_MESSAGE AS MESSAGE, "
                        + "  'INFO' AS LVL, CHK_TYPE "
                        + "FROM ( "
                        + "  SELECT * "
                        + "    FROM JAVIS.TB_EXCEPTION_LOG "
                        + "   ORDER BY CHK_DATE DESC, CHK_TIME DESC "
                        + ") EXLOG "
                        + "WHERE ROWNUM < 10 "
                        + "UNION ALL "
                        + "SELECT 'TBS' ALERT_TYPE, "
                        + "  TO_CHAR(SYSDATE, 'YYYY/MM/DD HH24:MI:SS') AS TIME, "
                        + "  (SELECT DB_DESCRIPT FROM JAVIS.TB_JAVIS_DB_LIST WHERE DB_TYPE=TS.DB_TYPE AND DB_NAME=TS.DB_NAME) AS DB_DESC, "
                        + "  'Tablespace ' || MAX(TS_NAME) || ' 사용률 ' || MAX(USED_RATE) || '% (임계치 ' || MAX(THRES_MB) || '%)' AS MESSAGE, "
                        + "  'ERROR' AS LVL, 'THRES' CHK_TYPE "
                        + "FROM JAVIS.VW_TS_SIZE_MON TS "
                        + "GROUP BY DB_TYPE, DB_NAME "
                        + ")";



        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AlertSummaryDto dto = new AlertSummaryDto();
            dto.setAlertType(rs.getString("ALERT_TYPE"));
            dto.setTime(rs.getString("TIME"));
            dto.setDbDesc(rs.getString("DB_DESC"));
            dto.setMessage(rs.getString("MESSAGE"));
            dto.setLvl(rs.getString("LVL"));
            dto.setChkType(rs.getString("CHK_TYPE"));
            return dto;
        });
    }
}