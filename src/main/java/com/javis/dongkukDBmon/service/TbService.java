// TbService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.TablespaceUsageDto;
import com.javis.dongkukDBmon.Dto.TsSummaryDto;
import com.javis.dongkukDBmon.Dto.TsSummaryRequestDto;
import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;
import com.javis.dongkukDBmon.repository.TablespaceUsageRepository;
import com.javis.dongkukDBmon.repository.TbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 클래스 안에 추가

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TbService {
    private static final Logger logger = LoggerFactory.getLogger(TbService.class);
    @Autowired
    private TbRepository tbRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TablespaceUsageRepository tablespaceUsageRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    // ID로 TiberoCap_Check_Mg 조회
    public Optional<TiberoCap_Check_Mg> getTbById(TiberoCapCheckMgId id) {
        return tbRepository.findById(id);
    }

    // 모든 TiberoCap_Check_Mg 데이터 조회
    public List<TiberoCap_Check_Mg> getAllTbs() {
        return tbRepository.findAll();
    }

    // 전체 DB 목록 가져오기
    public List<String> getTbList() {
        return tbRepository.findAllDbNames();
    }

    @Transactional
    public void refreshDbList() {
        try {
            logger.info("[refreshDbList] TRUNCATE TABLE 시작");
            jdbcTemplate.execute("TRUNCATE TABLE TB_DB_CAP_LIST");
            logger.info("[refreshDbList] TRUNCATE TABLE 성공");

            logger.info("[refreshDbList] INSERT INTO 시작");
            jdbcTemplate.execute("INSERT INTO TB_DB_CAP_LIST SELECT DISTINCT DB_NAME FROM TB_DB_CAP_CHECK_MG");
            logger.info("[refreshDbList] INSERT INTO 성공");
        } catch (Exception e) {
            logger.error("[refreshDbList] 에러 발생", e);
            throw e;  // 예외 다시 던지기 (컨트롤러에서 잡을 수 있게)
        }
    }

    // 특정 DB 이름과 시간 조건에 따른 테이블스페이스 데이터 조회
    @Transactional
    public List<TiberoCap_Check_Mg> getTablespaces(String dbName) {

        // 현재 시간에서 seoul기준
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // "yyyy/MM/dd HH:mm:ss" 형식으로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedTimeLimit = now.format(formatter);
        // 쿼리 실행
        List<TiberoCap_Check_Mg> results = tbRepository.findTablespacesByDbName(dbName);
        results.forEach(System.out::println);

        return results;
    }
    @Transactional
    public List<TiberoCap_Check_Mg> getRecentTablespaceDataForChat(String dbName, String tsName) {
        return tbRepository.findRecentDataForChat(dbName, tsName);
    }

    public List<TablespaceUsageDto> getMonthlyTsIncrease() {
        return tablespaceUsageRepository.getMonthlyUsage();
    }

    public List<TablespaceUsageDto> getWeeklyTsIncrease() {
        return tablespaceUsageRepository.getWeeklyUsage();
    }

    public List<TsSummaryDto> getTablespaceSummary(TsSummaryRequestDto request) {
        String sql =
                "WITH\n" +
                        "params AS (\n" +
                        "  SELECT \n" +
                        "    /* 증설/노이즈 파라미터 */\n" +
                        "    10240 AS add_abs_mb,\n" +
                        "    0.01  AS add_pct,\n" +
                        "    3     AS cooldown_days,\n" +
                        "    4     AS cooldown_big_days,\n" +
                        "    20480 AS big_add_mb,\n" +
                        "    7     AS min_inc_days,\n" +
                        "    90    AS pre_window_days,\n" +
                        "    21    AS post_window_days,\n" +
                        "    0.20  AS density_thresh,\n" +
                        "    21    AS gap_days_thresh,\n" +
                        "    0     AS conservative_mode,\n" +
                        "    /* 조회조건 파라미터: CHK_DATE가 VARCHAR2(8) 이므로 하이픈 제거 */\n" +
                        "    REPLACE(:startDate,'-','') AS start_date_vc,\n" +
                        "    REPLACE(:endDate,'-','')   AS end_date_vc,\n" +
                        "    :dbName                    AS target_db,\n" +
                        "    /* 오늘에서 9999-12-31까지 남은 '일수' 상한 */\n" +
                        "    (DATE '9999-12-31' - TRUNC(SYSDATE)) AS max_days_to_9999\n" +
                        "  FROM dual\n" +
                        "),\n" +
                        "\n" +
                        "/*** 1) 원자료 → 일자별 대표값 ***/\n" +
                        "base AS (\n" +
                        "  SELECT\n" +
                        "    DB_NAME, TS_NAME,\n" +
                        "    TO_DATE(CHK_DATE, 'YYYYMMDD') AS dt,\n" +
                        "    MAX(TOTAL_SIZE)          AS total_size_mb,\n" +
                        "    MAX(USED_SIZE)           AS used_size_mb,\n" +
                        "    MAX(MAXMBBYTES)          AS maxmbbytes_mb,\n" +
                        "    MAX(FREE_SIZE)           AS free_size_mb,\n" +
                        "    MAX(MAX_FREE_MB)         AS max_free_mb,\n" +
                        "    MAX(AUTOEXTENS_CNT_FILE) AS auto_cnt\n" +
                        "  FROM JAVIS.TB_DB_CAP_CHECK_MG\n" +
                        "  WHERE CHK_DATE BETWEEN (SELECT start_date_vc FROM params) AND (SELECT end_date_vc FROM params)\n" +
                        "    AND DB_NAME = (SELECT target_db FROM params)\n" +
                        "  GROUP BY DB_NAME, TS_NAME, TO_DATE(CHK_DATE,'YYYYMMDD')\n" +
                        "),\n" +
                        "daily AS (\n" +
                        "  SELECT\n" +
                        "    DB_NAME, TS_NAME, dt,\n" +
                        "    MAX(used_size_mb)                                AS used_day_mb,\n" +
                        "    GREATEST(MAX(total_size_mb), MAX(maxmbbytes_mb)) AS cap_day_mb,\n" +
                        "    MAX(free_size_mb)                                AS free_day_mb,\n" +
                        "    MAX(max_free_mb)                                 AS max_free_day_mb,\n" +
                        "    MAX(auto_cnt)                                    AS auto_cnt_day\n" +
                        "  FROM base\n" +
                        "  GROUP BY DB_NAME, TS_NAME, dt\n" +
                        "),\n" +
                        "\n" +
                        "/*** 2) cap 변경 감지 → 세그먼트 ***/\n" +
                        "cap_marks AS (\n" +
                        "  SELECT d.*,\n" +
                        "         LAG(cap_day_mb)   OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS prev_cap,\n" +
                        "         LAG(auto_cnt_day) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS prev_auto_cnt,\n" +
                        "         (cap_day_mb - LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) AS cap_delta_mb,\n" +
                        "         CASE\n" +
                        "           WHEN LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) IS NULL THEN 0\n" +
                        "           WHEN NVL(auto_cnt_day,0) >\n" +
                        "                NVL(LAG(auto_cnt_day) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt),0) THEN 1\n" +
                        "           WHEN (cap_day_mb - LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt))\n" +
                        "                >= GREATEST((SELECT add_abs_mb FROM params),\n" +
                        "                            ROUND(NVL(LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt),0)\n" +
                        "                                  * (SELECT add_pct FROM params))) THEN 1\n" +
                        "           ELSE 0\n" +
                        "         END AS cap_changed\n" +
                        "  FROM daily d\n" +
                        "),\n" +
                        "seg0 AS (\n" +
                        "  SELECT c.*,\n" +
                        "         SUM(cap_changed) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt\n" +
                        "                                ROWS UNBOUNDED PRECEDING) AS seg_id\n" +
                        "  FROM cap_marks c\n" +
                        "),\n" +
                        "seg AS (\n" +
                        "  SELECT\n" +
                        "    s0.*,\n" +
                        "    MAX(CASE WHEN cap_changed=1 THEN NVL(cap_delta_mb,0) END)\n" +
                        "      OVER (PARTITION BY DB_NAME, TS_NAME, seg_id) AS seg_add_mb\n" +
                        "  FROM seg0 s0\n" +
                        "),\n" +
                        "\n" +
                        "/*** 3) 증가량(+만), 쿨다운 반영 ***/\n" +
                        "growth AS (\n" +
                        "  SELECT\n" +
                        "    DB_NAME, TS_NAME, dt, cap_day_mb, cap_changed, NVL(seg_add_mb,0) AS seg_add_mb,\n" +
                        "    ROW_NUMBER() OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS day_no,\n" +
                        "    used_day_mb,\n" +
                        "    MIN(dt) OVER (PARTITION BY DB_NAME, TS_NAME, seg_id) AS seg_start_dt,\n" +
                        "    GREATEST(0, used_day_mb - LAG(used_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) AS inc_raw\n" +
                        "  FROM seg\n" +
                        "),\n" +
                        "growth2 AS (\n" +
                        "  SELECT g.*,\n" +
                        "         CASE\n" +
                        "           WHEN g.cap_changed=1 THEN NULL\n" +
                        "           WHEN g.dt < ( g.seg_start_dt\n" +
                        "                         + CASE WHEN g.seg_add_mb >= (SELECT big_add_mb FROM params)\n" +
                        "                                THEN (SELECT cooldown_big_days FROM params)\n" +
                        "                                ELSE (SELECT cooldown_days FROM params)\n" +
                        "                           END ) THEN NULL\n" +
                        "           WHEN g.inc_raw > 0 THEN g.inc_raw\n" +
                        "         END AS inc_mb_for_avg\n" +
                        "  FROM growth g\n" +
                        "),\n" +
                        "\n" +
                        "/*** 4) 직전 세그먼트 속도 ***/\n" +
                        "prev_key AS ( SELECT DB_NAME, TS_NAME, MAX(seg_id) AS last_seg_id FROM seg GROUP BY DB_NAME, TS_NAME ),\n" +
                        "prev_seg_rows AS (\n" +
                        "  SELECT s.* FROM seg s\n" +
                        "  JOIN prev_key k ON k.DB_NAME=s.DB_NAME AND k.TS_NAME=s.TS_NAME\n" +
                        "  WHERE s.seg_id = k.last_seg_id - 1\n" +
                        "),\n" +
                        "prev_growth AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, dt, used_day_mb,\n" +
                        "         ROW_NUMBER() OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt DESC) rn_desc,\n" +
                        "         GREATEST(0, used_day_mb - LAG(used_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) AS inc_raw\n" +
                        "  FROM prev_seg_rows\n" +
                        "),\n" +
                        "prev_win AS ( SELECT * FROM prev_growth WHERE rn_desc <= (SELECT pre_window_days FROM params) ),\n" +
                        "prev_speed AS (\n" +
                        "  SELECT\n" +
                        "    DB_NAME, TS_NAME,\n" +
                        "    NVL(NULLIF(AVG(CASE WHEN inc_raw>0 THEN inc_raw END),0),\n" +
                        "        CASE WHEN REGR_SLOPE(used_day_mb, rn_desc) > 0\n" +
                        "             THEN REGR_SLOPE(used_day_mb, rn_desc) END) AS speed_pre,\n" +
                        "    COUNT(CASE WHEN inc_raw>0 THEN 1 END) AS pos_inc_days_pre,\n" +
                        "    COUNT(*) AS regr_sample_days_pre\n" +
                        "  FROM prev_win\n" +
                        "  GROUP BY DB_NAME, TS_NAME\n" +
                        "),\n" +
                        "\n" +
                        "/*** 5) as-of 창 계산 ***/\n" +
                        "asof_calc AS (\n" +
                        "  SELECT\n" +
                        "    p.DB_NAME, p.TS_NAME, p.dt,\n" +
                        "    p.cap_day_mb  AS cap_asof_mb,\n" +
                        "    p.used_day_mb AS used_asof_mb,\n" +
                        "\n" +
                        "    /* 최근 N일 P90 윈저 평균 */\n" +
                        "    (SELECT AVG( LEAST( x.inc_mb_for_avg,\n" +
                        "                        (SELECT PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY y.inc_mb_for_avg)\n" +
                        "                           FROM growth2 y\n" +
                        "                          WHERE y.DB_NAME=x.DB_NAME AND y.TS_NAME=x.TS_NAME\n" +
                        "                            AND y.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "                            AND y.dt <= p.dt\n" +
                        "                            AND y.inc_mb_for_avg IS NOT NULL) ) )\n" +
                        "       FROM growth2 x\n" +
                        "      WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "        AND x.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "        AND x.dt <= p.dt\n" +
                        "        AND x.inc_mb_for_avg IS NOT NULL) AS avg_pos_inc_win,\n" +
                        "\n" +
                        "    (SELECT COUNT(x.inc_mb_for_avg)\n" +
                        "       FROM growth2 x\n" +
                        "      WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "        AND x.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "        AND x.dt <= p.dt\n" +
                        "        AND x.inc_mb_for_avg IS NOT NULL) AS pos_inc_days_win,\n" +
                        "\n" +
                        "    (SELECT COUNT(*)\n" +
                        "       FROM growth2 x\n" +
                        "      WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "        AND x.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "        AND x.dt <= p.dt) AS sample_days_win,\n" +
                        "\n" +
                        "    (SELECT REGR_SLOPE(x.used_day_mb, x.day_no)\n" +
                        "       FROM growth2 x\n" +
                        "      WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "        AND x.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "        AND x.dt <= p.dt) AS slope_win,\n" +
                        "\n" +
                        "    (SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY x.inc_mb_for_avg)\n" +
                        "       FROM growth2 x\n" +
                        "      WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "        AND x.dt > p.dt - (SELECT post_window_days FROM params)\n" +
                        "        AND x.dt <= p.dt\n" +
                        "        AND x.inc_mb_for_avg IS NOT NULL) AS med_pos_inc_win,\n" +
                        "\n" +
                        "    NVL( (SELECT MAX(p.dt - x.dt)\n" +
                        "            FROM growth2 x\n" +
                        "           WHERE x.DB_NAME=p.DB_NAME AND x.TS_NAME=p.TS_NAME\n" +
                        "             AND x.dt <= p.dt\n" +
                        "             AND x.inc_mb_for_avg IS NOT NULL), 9999) AS last_pos_gap_days\n" +
                        "  FROM growth2 p\n" +
                        "),\n" +
                        "\n" +
                        "/*** 6) 최종 속도 ***/\n" +
                        "final_speed AS (\n" +
                        "  SELECT a.DB_NAME, a.TS_NAME, a.dt,\n" +
                        "         a.cap_asof_mb, a.used_asof_mb,\n" +
                        "         a.avg_pos_inc_win, a.pos_inc_days_win, a.sample_days_win,\n" +
                        "         a.slope_win, a.med_pos_inc_win, a.last_pos_gap_days,\n" +
                        "         ps.speed_pre, ps.pos_inc_days_pre, ps.regr_sample_days_pre,\n" +
                        "         NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0), 0) AS density_win,\n" +
                        "         LEAST(\n" +
                        "           NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END),\n" +
                        "           NVL(a.med_pos_inc_win * NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0),0), 1e18)\n" +
                        "         ) AS speed_post_robust,\n" +
                        "         CASE\n" +
                        "           WHEN a.last_pos_gap_days <= 14\n" +
                        "                AND NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0), 0) >= 0.30\n" +
                        "           THEN GREATEST(\n" +
                        "                  NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END),\n" +
                        "                  NVL(a.med_pos_inc_win, 0)\n" +
                        "                )\n" +
                        "           WHEN NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0), 0) < (SELECT density_thresh FROM params)\n" +
                        "             OR a.last_pos_gap_days > (SELECT gap_days_thresh FROM params)\n" +
                        "           THEN NVL(ps.speed_pre,\n" +
                        "                    LEAST(\n" +
                        "                      NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END),\n" +
                        "                      NVL(a.med_pos_inc_win * NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0),0), 1e18)\n" +
                        "                    )\n" +
                        "                ) * NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0), 0)\n" +
                        "           ELSE LEAST(\n" +
                        "                  NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END),\n" +
                        "                  NVL(a.med_pos_inc_win * NVL(a.pos_inc_days_win / NULLIF(a.sample_days_win,0),0), 1e18)\n" +
                        "                )\n" +
                        "         END AS speed_final\n" +
                        "  FROM asof_calc a\n" +
                        "  LEFT JOIN prev_speed ps\n" +
                        "    ON ps.DB_NAME=a.DB_NAME AND ps.TS_NAME=a.TS_NAME\n" +
                        "),\n" +
                        "\n" +
                        "/*** 7) 마지막 스냅샷 ***/\n" +
                        "last_vals AS (\n" +
                        "  SELECT d1.DB_NAME, d1.TS_NAME, d1.dt AS last_dt,\n" +
                        "         d1.used_day_mb      AS curr_used_mb,\n" +
                        "         d1.cap_day_mb       AS curr_cap_mb,\n" +
                        "         d1.free_day_mb      AS curr_free_mb,\n" +
                        "         d1.max_free_day_mb  AS curr_max_free_mb\n" +
                        "  FROM daily d1\n" +
                        "  WHERE d1.dt = (SELECT MAX(d2.dt) FROM daily d2\n" +
                        "                 WHERE d2.DB_NAME=d1.DB_NAME AND d2.TS_NAME=d1.TS_NAME)\n" +
                        "),\n" +
                        "\n" +
                        "/*** 8) 기간 내 실사용 증가 합(증설/노이즈 제거 후 +만) ***/\n" +
                        "real_used AS (\n" +
                        "  SELECT DB_NAME, TS_NAME,\n" +
                        "         SUM(inc_mb_for_avg) AS real_used_mb\n" +
                        "  FROM growth2\n" +
                        "  WHERE inc_mb_for_avg IS NOT NULL\n" +
                        "  GROUP BY DB_NAME, TS_NAME\n" +
                        ")\n" +
                        "\n" +
                        "/*** 9) 화면 최종 산출 ***/\n" +
                        "SELECT\n" +
                        "  f.DB_NAME,\n" +
                        "  f.TS_NAME,\n" +
                        "  f.cap_asof_mb                              AS TOTAL_SIZE_MB,\n" +
                        "  NVL(r.real_used_mb,0)                      AS REAL_USED_MB,\n" +
                        "  ROUND( lv.curr_used_mb / NULLIF(f.cap_asof_mb, 0) * 100, 2 ) AS TOT_USAGE_PERCENT,\n" +
                        "  NVL(lv.curr_max_free_mb, lv.curr_free_mb)  AS REMAIN_MB,\n" +
                        "  ROUND(\n" +
                        "    NVL(r.real_used_mb,0)\n" +
                        "    / NULLIF( SUM(NVL(r.real_used_mb,0)) OVER (PARTITION BY f.DB_NAME), 0 ) * 100\n" +
                        "  , 2)                                       AS REAL_USED_PERCENT,\n" +
                        "\n" +
                        "  /* 일수(보조) — 9999-12-31을 넘지 않도록 상한 적용 */\n" +
                        "  CASE\n" +
                        "    WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "    WHEN NVL(lv.curr_max_free_mb, lv.curr_free_mb) <= 0 THEN 0\n" +
                        "    ELSE LEAST(\n" +
                        "           CEIL( NVL(lv.curr_max_free_mb, lv.curr_free_mb) / f.speed_final ),\n" +
                        "           (SELECT max_days_to_9999 FROM params)\n" +
                        "         )\n" +
                        "  END AS DAYS_TO_FULL,\n" +
                        "\n" +
                        "  CASE\n" +
                        "    WHEN lv.curr_used_mb >= (f.cap_asof_mb * 0.95) THEN 0\n" +
                        "    WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "    ELSE LEAST(\n" +
                        "           CEIL( GREATEST(ROUND(f.cap_asof_mb*0.95) - lv.curr_used_mb, 0) / f.speed_final ),\n" +
                        "           (SELECT max_days_to_9999 FROM params)\n" +
                        "         )\n" +
                        "  END AS DAYS_TO_95PERCENT,\n" +
                        "\n" +
                        "  /* 날짜(표시용) — 날짜 더하기 전에 일수 상한 적용 */\n" +
                        "  TO_CHAR(\n" +
                        "    CASE\n" +
                        "      WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "      WHEN NVL(lv.curr_max_free_mb, lv.curr_free_mb) <= 0 THEN TRUNC(SYSDATE)\n" +
                        "      ELSE TRUNC(SYSDATE)\n" +
                        "           + LEAST(\n" +
                        "               CEIL( NVL(lv.curr_max_free_mb, lv.curr_free_mb) / f.speed_final ),\n" +
                        "               (SELECT max_days_to_9999 FROM params)\n" +
                        "             )\n" +
                        "    END, 'YYYY-MM-DD'\n" +
                        "  ) AS FULL_REACH_DATE,\n" +
                        "\n" +
                        "  TO_CHAR(\n" +
                        "    CASE\n" +
                        "      WHEN lv.curr_used_mb >= (f.cap_asof_mb * 0.95) THEN TRUNC(SYSDATE)\n" +
                        "      WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "      ELSE TRUNC(SYSDATE)\n" +
                        "           + LEAST(\n" +
                        "               CEIL( GREATEST(ROUND(f.cap_asof_mb*0.95) - lv.curr_used_mb, 0) / f.speed_final ),\n" +
                        "               (SELECT max_days_to_9999 FROM params)\n" +
                        "             )\n" +
                        "    END, 'YYYY-MM-DD'\n" +
                        "  ) AS REACH_95P_DATE\n" +
                        "\n" +
                        "FROM final_speed f\n" +
                        "JOIN last_vals lv ON lv.DB_NAME=f.DB_NAME AND lv.TS_NAME=f.TS_NAME AND lv.last_dt=f.dt\n" +
                        "LEFT JOIN real_used r ON r.DB_NAME=f.DB_NAME AND r.TS_NAME=f.TS_NAME\n" +
                        "ORDER BY f.DB_NAME, f.TS_NAME";

        Map<String, Object> params = new HashMap<>();
        params.put("dbName", request.getDbName());
        params.put("startDate", request.getStartDate());
        params.put("endDate", request.getEndDate());

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new TsSummaryDto(
                        rs.getString("DB_NAME"),
                        rs.getString("TS_NAME"),
                        rs.getDouble("TOTAL_SIZE_MB"),
                        rs.getDouble("TOT_USAGE_PERCENT"),
                        rs.getDouble("REAL_USED_MB"),
                        rs.getDouble("REAL_USED_PERCENT"),
                        rs.getDouble("REMAIN_MB"),
                        getNullableLong(rs, "DAYS_TO_FULL"),
                        getNullableLong(rs, "DAYS_TO_95PERCENT"),
                        rs.getString("FULL_REACH_DATE"),
                        rs.getString("REACH_95P_DATE")
                )
        );
    }

    private Long getNullableLong(ResultSet rs, String col) throws SQLException {
        java.math.BigDecimal bd = rs.getBigDecimal(col);
        return (bd == null) ? null : bd.longValue();
    }

}
